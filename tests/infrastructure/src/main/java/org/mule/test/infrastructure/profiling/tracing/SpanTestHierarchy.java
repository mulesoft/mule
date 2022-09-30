/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.profiling.tracing;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A tree-based hierarchy to model the span hierarchy that must be expected on tests.
 *
 * @since 4.5.0
 */
public class SpanTestHierarchy {

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

  /**
   * Traverses the expected span hierarchy tree asserting that each node exists in the actual captured spans and that it has the
   * correct parent node
   * 
   * @param rootNode the root node from where to start the assertion
   */
  public void assertSpanTree(SpanNode rootNode) {
    assertSpanTree(rootNode, null);
  }

  private void assertSpanTree(SpanNode expectedNode, CapturedExportedSpan actualParent) {
    CapturedExportedSpan actualSpan = findActualSpan(expectedNode, actualParent);
    for (SpanNode expectedChild : expectedNode.children) {
      assertSpanTree(expectedChild, actualSpan);
    }
  }

  private CapturedExportedSpan findActualSpan(SpanNode expectedNode, CapturedExportedSpan actualParent) {
    CapturedExportedSpan actualSpan = actualExportedSpans.stream()
        .filter(exportedSpan -> !visitedSpans.contains(exportedSpan.getSpanId())
            && exportedSpan.getName().equals(expectedNode.spanName)
            && hasCorrectParent(exportedSpan, actualParent != null ? actualParent.getName() : null))
        .findFirst().orElse(null);
    assertThat("Expected span: " + expectedNode.spanName + " was not found", actualSpan, notNullValue());
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

  private static class SpanNode {

    private final String spanName;
    private SpanNode parent;
    private final List<SpanNode> children = new ArrayList<>();

    public SpanNode(String spanName) {
      this.spanName = spanName;
    }

    public void addChild(SpanNode child) {
      children.add(child);
    }
  }
}
