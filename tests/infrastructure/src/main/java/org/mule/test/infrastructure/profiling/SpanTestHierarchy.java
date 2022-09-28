/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SpanTestHierarchy {

  private SpanNode root;
  private SpanNode currentNode;
  private SpanNode lastChild;
  private final String NO_PARENT_SPAN = "0000000000000000";
  private HashSet<String> visitedSpans = new HashSet();
  private HashMap<String, CapturedExportedSpan> spanHashMap = new HashMap<>();

  public class SpanNode {

    private String spanName;
    private SpanNode parent;
    private List<SpanNode> children = new ArrayList<>();

    public SpanNode(String spanName) {
      this.spanName = spanName;
    }

    public void addChild(SpanNode child) {
      children.add(child);
    }

    public List<SpanNode> getChildren() {
      return children;
    }
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
    if (currentNode.parent != null) {
      lastChild = lastChild.parent;
      currentNode = currentNode.parent.parent;
    }
    return this;
  }

  public SpanNode getRoot() {
    return root;
  }

  public void assertRoot(SpanNode node, CapturedExportedSpan span) {
    assertThat(span, notNullValue());
    assertThat(span.getName(), equalTo(node.spanName));
    assertThat(span.getParentSpanId(), equalTo(NO_PARENT_SPAN));
  } //TODO:Check redundancy

  public void assertPreOrder(SpanNode node, CapturedExportedSpan parent, Collection<CapturedExportedSpan> exportedSpans) {
    if (spanHashMap.isEmpty()) {
      exportedSpans.forEach(span -> spanHashMap.put(span.getSpanId(), span));
    }
    if (node != null) {
      CapturedExportedSpan capturedExportedSpan;
      if (node.spanName.equals("mule:flow")) {
        capturedExportedSpan = findExpectedSpan(node, null, exportedSpans);
      } else {
        capturedExportedSpan = findExpectedSpan(node, parent, exportedSpans);
      }
      for (SpanNode child : node.children) {
        assertPreOrder(child, capturedExportedSpan, exportedSpans);
      }
    }
  } 

  private CapturedExportedSpan findExpectedSpan(SpanNode spanNode, CapturedExportedSpan parent,
                                                Collection<CapturedExportedSpan> exportedSpans) {
    CapturedExportedSpan expectedSpan = exportedSpans.stream()
        .filter(exportedSpan -> !visitedSpans.contains(exportedSpan.getSpanId())
            && exportedSpan.getName().equals(spanNode.spanName)
            && findCorrectParentInMap(exportedSpan, parent != null ? parent.getName() : null))
        .findFirst().orElse(null);
    assertThat("Expected span: " + spanNode.spanName + " was not found", expectedSpan, notNullValue());
    visitedSpans.add(expectedSpan.getSpanId());
    return expectedSpan;
  }

  private boolean findCorrectParent(Collection<CapturedExportedSpan> exportedSpans, CapturedExportedSpan expectedSpan,
                                    String expectedParentName) {
    CapturedExportedSpan parentSpan = exportedSpans.stream()
        .filter(exportedSpan -> exportedSpan.getSpanId().equals(expectedSpan.getParentSpanId()))
        .findFirst().orElse(null);
    if (expectedParentName != null && parentSpan == null) {
      return false;
    } else if (expectedParentName == null && parentSpan == null) {
      return true;
    }
    return parentSpan.getName().equals(expectedParentName);
  }

  private boolean findCorrectParentInMap(CapturedExportedSpan expectedSpan, String expectedParentName) {
    CapturedExportedSpan parentSpan = spanHashMap.get(expectedSpan.getParentSpanId());
    if (expectedParentName != null && parentSpan == null) {
      return false;
    } else if (expectedParentName == null && parentSpan == null) {
      return true;
    }
    return parentSpan.getName().equals(expectedParentName);
  }
}
