/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling.tracing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedEventData;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;

/**
 * A tree-based hierarchy to model the span hierarchy that must be expected on tests.
 *
 * @since 4.5.0
 */
public class SpanTestHierarchy {

  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String THREAD_START_ID_KEY = "threadStartId";
  public static final String LOCATION_KEY = "location";
  public static final String CORRELATION_ID_KEY = "correlationId";

  public static final String OTEL_EXCEPTION_TYPE_KEY = "exception.type";
  public static final String OTEL_EXCEPTION_MESSAGE_KEY = "exception.message";
  public static final String OTEL_EXCEPTION_STACK_TRACE_KEY = "exception.stacktrace";
  public static final String OTEL_EXCEPTION_ESCAPED_KEY = "exception.escaped";
  public static final String OTEL_EXCEPTION_EVENT_NAME = "exception";

  public static final String NO_EXCEPTION = "NONE";

  private SpanNode root;
  private SpanNode currentNode;
  private SpanNode lastChild;
  private static final String NO_PARENT_SPAN = "0000000000000000";
  private final HashSet<String> visitedSpans = new HashSet();
  private final HashMap<String, CapturedExportedSpan> spanHashMap = new HashMap<>();
  private final Collection<CapturedExportedSpan> actualExportedSpans;

  public SpanTestHierarchy(Collection<CapturedExportedSpan> actualExportedSpans) {
    this.actualExportedSpans = actualExportedSpans;
    actualExportedSpans.forEach(span -> spanHashMap.put(span.getSpanId(), span));
  }

  public SpanTestHierarchy withRoot(String rootName) {
    return withRoot(rootName, new HashMap<>());
  }

  public SpanTestHierarchy withRoot(String rootName, Map<String, String> attributes) {
    root = new SpanNode(rootName);
    root.addAttributes(attributes);
    root.parent = new SpanNode(NO_PARENT_SPAN);
    currentNode = root;
    return this;
  }

  public SpanTestHierarchy beginChildren() {
    lastChild = currentNode;
    return this;
  }

  public SpanTestHierarchy child(String childName) {
    return child(childName, new HashMap<>());
  }

  public SpanTestHierarchy child(String childName, Map<String, String> attributes) {
    SpanNode child = new SpanNode(childName);
    child.parent = lastChild;
    child.addAttributes(attributes);
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

  public SpanTestHierarchy addExceptionData(String exceptionData) {
    currentNode.setExceptionData(exceptionData);
    return this;
  }

  public SpanTestHierarchy noExceptionExpected() {
    currentNode.setExceptionData("NONE");
    return this;
  }

  public SpanNode getRoot() {
    return root;
  }

  /**
   * Traverses the expected span hierarchy tree asserting that each node exists in the actual captured spans and that it has the
   * correct parent node
   *
   */
  public void assertSpanTree() {
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
    assertAttributes(actualSpan, expectedNode);
    assertException(actualSpan, expectedNode);
    visitedSpans.add(actualSpan.getSpanId());
    return actualSpan;
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

  private boolean hasCorrectLocation(CapturedExportedSpan span, String expectedLocation) {
    return expectedLocation == null || span.getAttributes().get(LOCATION_KEY).equals(expectedLocation);
  }

  private void assertAttributes(CapturedExportedSpan actualSpan, SpanNode expectedNode) {
    expectedNode.getAttributes()
        .forEach((key, value) -> assertThat(
                                            "Actual attribute \"" + key + "\" for: " + expectedNode.spanName
                                                + " is not the expected one",
                                            actualSpan.getAttributes().get(key), equalTo(value)));
    assertThat(actualSpan.getAttributes().get(CORRELATION_ID_KEY), notNullValue());
    assertThat(actualSpan.getAttributes().get(THREAD_START_ID_KEY), notNullValue());
    assertThat(actualSpan.getServiceName(), equalTo(actualSpan.getAttributes().get(ARTIFACT_ID_KEY)));
  }

  private void assertException(CapturedExportedSpan actualSpan, SpanNode expectedNode) {
    if (expectedNode.getExceptionData() == null) {
      return;
    }
    if (expectedNode.getExceptionData().equals(NO_EXCEPTION)) {
      assertThat(String.format("Unexpected Span exceptions found for Span: [%s]", actualSpan),
                 actualSpan.getEvents().size(), equalTo(0));
    } else {
      List<CapturedEventData> exceptions = actualSpan.getEvents().stream()
          .filter(capturedEventData -> capturedEventData.getName().equals(OTEL_EXCEPTION_EVENT_NAME))
          .collect(Collectors.toList());
      assertThat(String.format("Expected exceptions for Span: [%s] differ", actualSpan), exceptions.size(), equalTo(1));
      assertExceptionAttributes(exceptions.iterator().next(), expectedNode.getExceptionData());
      assertThat(actualSpan.hasErrorStatus(), is(true));
    }
  }

  public void assertExceptionAttributes(CapturedEventData exceptionData, String errorType) {
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY), CoreMatchers.equalTo(errorType));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_MESSAGE_KEY), CoreMatchers.equalTo("An error occurred."));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_ESCAPED_KEY), CoreMatchers.equalTo("true"));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_STACK_TRACE_KEY).toString(), not(emptyOrNullString()));
  }

  private static class SpanNode {

    private final String spanName;
    private SpanNode parent;
    private final List<SpanNode> children = new ArrayList<>();
    private Map<String, String> attributes;
    private String exceptionData;

    public SpanNode(String spanName) {
      this.spanName = spanName;
    }

    public void addChild(SpanNode child) {
      children.add(child);
    }

    public void addAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }

    public void setExceptionData(String exceptionData) {
      this.exceptionData = exceptionData;
    }

    public String getAttribute(String key) {
      return attributes.get(key);
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public String getExceptionData() {
      return exceptionData;
    }
  }
}
