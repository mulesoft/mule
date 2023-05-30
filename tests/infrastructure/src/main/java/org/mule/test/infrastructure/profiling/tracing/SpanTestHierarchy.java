/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling.tracing;

import static org.junit.Assert.fail;
import static org.mule.test.infrastructure.profiling.tracing.ExceptionEventMatcher.OTEL_EXCEPTION_EVENT_NAME;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.test.infrastructure.profiling.tracing.ExceptionEventMatcher.errorType;

import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;

/**
 * A tree-based hierarchy to model the span hierarchy that must be expected on tests.
 *
 * @since 4.5.0
 */
public class SpanTestHierarchy {

  public static final String LOCATION_KEY = "location";
  public static final String UNSET_STATUS_CODE = "UNSET";
  private SpanNode root;
  private SpanNode currentNode;
  private SpanNode lastChild;
  private static final String NO_PARENT_SPAN = "0000000000000000";
  private final HashSet<String> visitedSpans = new HashSet<>();
  private final HashMap<String, CapturedExportedSpan> spanHashMap = new HashMap<>();
  private Collection<CapturedExportedSpan> actualExportedSpans;

  public SpanTestHierarchy(Collection<CapturedExportedSpan> actualExportedSpans) {
    this.actualExportedSpans = actualExportedSpans;
    actualExportedSpans.forEach(span -> spanHashMap.put(span.getSpanId(), span));
  }

  public SpanTestHierarchy() {
    new SpanTestHierarchy(Collections.emptyList());
  }

  public int size() {
    return 1 + size(root);
  }

  private int size(SpanNode current) {
    return current.children.size() + current.children.stream().map(this::size).reduce(0, Integer::sum);
  }

  public SpanTestHierarchy withCapturedSpans(Collection<CapturedExportedSpan> actualExportedSpans) {
    this.actualExportedSpans = actualExportedSpans;
    actualExportedSpans.forEach(span -> spanHashMap.put(span.getSpanId(), span));
    return this;
  }

  public SpanTestHierarchy withRoot(String rootName) {
    root = new SpanNode(rootName);
    root.parent = new SpanNode(NO_PARENT_SPAN);
    currentNode = root;
    return this;
  }

  public SpanTestHierarchy beginChildren() {
    lastChild = currentNode;
    return this;
  }

  public SpanTestHierarchy child(String childName) {
    SpanNode child = new SpanNode(childName);
    child.parent = lastChild;
    lastChild.addChild(child);
    currentNode = child;
    return this;
  }

  public SpanTestHierarchy endChildren() {
    if (currentNode != null && currentNode.parent != null) {
      lastChild = lastChild.parent;
      currentNode = currentNode.parent.parent;
    }
    return this;
  }

  public SpanTestHierarchy addAttributeToAssertValue(String key, String value) {
    currentNode.addAttributeThatShouldMatch(key, value);
    return this;
  }

  public SpanTestHierarchy addAttributesToAssertValue(Map<String, String> attributes) {
    currentNode.addAttributesThatShouldMatch(attributes);
    return this;
  }

  public SpanTestHierarchy addAttributesToAssertExistence(List<String> attributeNames) {
    currentNode.addAttributeThatShouldExist(attributeNames);
    return this;
  }

  public SpanTestHierarchy addAttributesToAssertExistence(String... attributeNames) {
    currentNode.addAttributeThatShouldExist(Arrays.asList(attributeNames));
    return this;
  }

  public SpanTestHierarchy addExceptionData(String errorType, String errorDescription) {
    currentNode.expectException(errorType, errorDescription);
    return this;
  }

  public SpanTestHierarchy addExceptionData(String errorType, String errorDescription, String errorStacktrace) {
    currentNode.expectException(errorType, errorDescription, errorStacktrace);
    return this;
  }

  public SpanTestHierarchy addExceptionData(String errorType) {
    currentNode.expectException(errorType);
    return this;
  }

  public SpanNode getRoot() {
    return root;
  }

  /**
   * Traverses the expected span hierarchy tree asserting that each node exists in the actual captured spans and that it has the
   * correct parent node
   */
  public void assertSpanTree() {
    visitedSpans.clear();
    assertSpanTree(root, null);
  }

  private void assertSpanTree(SpanNode expectedNode, CapturedExportedSpan actualParent) {
    CapturedExportedSpan actualSpan = assertActualSpan(expectedNode, actualParent);
    for (SpanNode expectedChild : expectedNode.children) {
      assertSpanTree(expectedChild, actualSpan);
    }
  }

  private CapturedExportedSpan assertActualSpan(SpanNode expectedNode, CapturedExportedSpan actualParent) {
    CapturedExportedSpan actualSpan = actualExportedSpans.stream()
        .filter(exportedSpan -> !visitedSpans.contains(exportedSpan.getSpanId())
            && exportedSpan.getName().equals(expectedNode.spanName)
            && hasCorrectLocation(exportedSpan, expectedNode.getAttribute(LOCATION_KEY))
            && hasCorrectParent(exportedSpan, actualParent != null ? actualParent.getName() : null))
        .findFirst().orElse(null);
    assertThat("Expected span: " + expectedNode.spanName + " was not found", actualSpan, notNullValue());
    assertTrue("Expected span: " + expectedNode.spanName + " has a different trace ID than parent",
               hasCorrectTraceId(actualSpan, actualParent != null ? actualParent.getName() : null));
    assertAttributes(actualSpan, expectedNode);
    assertTraceState(actualSpan, expectedNode);
    assertException(actualSpan, expectedNode);
    assertThat("Expected span: " + expectedNode.spanName + " has incorrect start or end time",
               actualSpan.getStartEpochSpanNanos(),
               is(lessThan(actualSpan.getEndSpanEpochNanos())));
    visitedSpans.add(actualSpan.getSpanId());
    return actualSpan;
  }

  private void assertTraceState(CapturedExportedSpan actualSpan, SpanNode expectedNode) {
    expectedNode.getTraceStateEntriesToAssert().forEach((key, value) -> {
      String capturedTraceValue = actualSpan.getTraceState().get(key);
      if (capturedTraceValue == null) {
        fail("The span " + expectedNode.spanName + " has no trace state key " + key);
      }

      assertThat("The span " + expectedNode.spanName + " has expected value " + value + " for key " + key + ". The value was "
          + capturedTraceValue, capturedTraceValue, equalTo(value));
    });

    expectedNode.getTraceStateKeyExistence().forEach(key -> {
      String capturedTraceValue = actualSpan.getTraceState().get(key);

      if (capturedTraceValue == null) {
        fail("The span " + expectedNode.spanName + " has no trace state key " + key);
      }
    });

    expectedNode.getTraceStateKeyNotExistence().forEach(key -> {
      String capturedTraceValue = actualSpan.getTraceState().get(key);

      if (capturedTraceValue != null) {
        fail("The span " + expectedNode.spanName + " has trace state key " + key + " and it must not be present");
      }
    });
  }

  private boolean hasCorrectParent(CapturedExportedSpan span, String expectedParentName) {
    CapturedExportedSpan parentSpan = spanHashMap.get(span.getParentSpanId());
    if (expectedParentName != null && parentSpan == null) {
      return false;
    } else if (expectedParentName == null && parentSpan == null) {
      return span.getParentSpanId().equals(NO_PARENT_SPAN);
    }
    return parentSpan.getName().equals(expectedParentName);
  }

  private boolean hasCorrectTraceId(CapturedExportedSpan span, String expectedParentName) {
    CapturedExportedSpan parentSpan = spanHashMap.get(span.getParentSpanId());
    if (expectedParentName != null && parentSpan == null) {
      return false;
    } else if (expectedParentName == null && parentSpan == null) {
      // When it's a root span there is no trace ID to compare to
      return true;
    }
    return parentSpan.getTraceId().equals(span.getTraceId());
  }

  private boolean hasCorrectLocation(CapturedExportedSpan span, String expectedLocation) {
    return expectedLocation == null || span.getAttributes().get(LOCATION_KEY).equals(expectedLocation);
  }

  private void assertAttributes(CapturedExportedSpan actualSpan, SpanNode expectedNode) {
    expectedNode.getAttributesThatShouldMatch()
        .forEach((key, value) -> assertThat(
                                            "Actual attribute \"" + key + "\" for: " + expectedNode.spanName
                                                + " is not the expected one",
                                            actualSpan.getAttributes().get(key), equalTo(value)));
    expectedNode.getAttributesThatShouldExist().forEach(attribute -> assertThat(
                                                                                "Actual attribute \"" + attribute + "\" for: "
                                                                                    + expectedNode.spanName
                                                                                    + " does not exist",
                                                                                actualSpan.getAttributes().get(attribute),
                                                                                notNullValue()));
  }

  private void assertException(CapturedExportedSpan actualSpan, SpanNode expectedNode) {
    expectedNode.assertExceptions(actualSpan);
  }

  public SpanTestHierarchy addTraceStateKeyValueAssertion(String key, String value) {
    currentNode.addTraceStateKeyValueToAssert(key, value);
    return this;
  }

  public SpanTestHierarchy addTraceStateKeyNotPresentAssertion(String key) {
    currentNode.addTraceStateKeyAssertNotExistence(key);
    return this;
  }

  public SpanTestHierarchy addTraceStateKeyPresentAssertion(String key) {
    currentNode.addTraceStateKeyAssertExistence(key);
    return this;
  }

  private static class SpanNode {

    private final String spanName;
    private SpanNode parent;
    private final List<SpanNode> children = new ArrayList<>();
    private final Map<String, String> attributesThatShouldMatch = new HashMap<>();
    private final List<String> attributesThatShouldExist = new ArrayList<>();
    private Matcher<CapturedEventData> exceptionEventMatcher;

    private final Map<String, String> traceStateEntriesToAssert = new HashMap<>();
    private List<String> traceStateKeyExistence = new ArrayList<>();
    private List<String> traceStateKeyNotExistence = new ArrayList<>();

    public SpanNode(String spanName) {
      this.spanName = spanName;
    }

    public void addChild(SpanNode child) {
      children.add(child);
    }

    public void addAttributeThatShouldMatch(String key, String value) {
      this.attributesThatShouldMatch.put(key, value);
    }

    public void addAttributesThatShouldMatch(Map<String, String> attributesThatShouldMatch) {
      this.attributesThatShouldMatch.putAll(attributesThatShouldMatch);
    }

    public void addAttributeThatShouldExist(List<String> attributes) {
      this.attributesThatShouldExist.addAll(attributes);
    }

    public void expectException(String errorType, String errorDescription) {
      this.exceptionEventMatcher = errorType(errorType).errorDescription(errorDescription);
    }

    public void expectException(String errorType, String errorDescription, String errorStacktrace) {
      this.exceptionEventMatcher = errorType(errorType).errorDescription(errorDescription).stackTrace(errorStacktrace);
    }

    public void expectException(String errorType) {
      this.exceptionEventMatcher = errorType(errorType);
    }

    public String getAttribute(String key) {
      return attributesThatShouldMatch.get(key);
    }

    public Map<String, String> getAttributesThatShouldMatch() {
      return attributesThatShouldMatch;
    }

    public List<String> getAttributesThatShouldExist() {
      return attributesThatShouldExist;
    }

    public void assertExceptions(CapturedExportedSpan actualSpan) {
      List<CapturedEventData> exceptionEvents = actualSpan.getEvents().stream()
          .filter(capturedEventData -> capturedEventData.getName().equals(OTEL_EXCEPTION_EVENT_NAME)).collect(toList());
      if (exceptionEventMatcher == null) {
        assertThat(format("Unexpected Span exceptions found for Span: [%s]", actualSpan),
                   exceptionEvents.size(), equalTo(0));
        assertThat(actualSpan.getStatusAsString(), is(UNSET_STATUS_CODE));
      } else {
        assertThat(format("Expected exceptions for Span: [%s] differ", actualSpan), exceptionEvents,
                   containsInAnyOrder(exceptionEventMatcher));
        assertThat(actualSpan.hasErrorStatus(), is(true));
      }
    }

    public void addTraceStateKeyValueToAssert(String key, String value) {
      traceStateEntriesToAssert.put(key, value);
    }

    public Map<String, String> getTraceStateEntriesToAssert() {
      return traceStateEntriesToAssert;
    }

    public List<String> getTraceStateKeyExistence() {
      return traceStateKeyExistence;
    }

    public List<String> getTraceStateKeyNotExistence() {
      return traceStateKeyNotExistence;
    }

    public void addTraceStateKeyAssertNotExistence(String key) {
      this.traceStateKeyNotExistence.add(key);
    }

    public void addTraceStateKeyAssertExistence(String key) {
      this.traceStateKeyExistence.add(key);
    }
  }
}
