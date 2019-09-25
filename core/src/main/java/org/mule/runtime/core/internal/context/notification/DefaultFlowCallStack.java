/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.lang.System.lineSeparator;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;

/**
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack {

  private static final long serialVersionUID = -8683711977929802819L;

  private final Deque<FlowStackElement> innerStack;

  public DefaultFlowCallStack() {
    this.innerStack = new ArrayDeque<>(4);
  }

  private DefaultFlowCallStack(final Deque<FlowStackElement> innerStack) {
    this.innerStack = new ArrayDeque<>(innerStack);
  }

  /**
   * Adds an element to the top of this stack
   *
   * @param flowStackElement the element to add
   */
  public void push(FlowStackElement flowStackElement) {
    innerStack.push(flowStackElement);
  }

  /**
   * Adds a message processor path to the list of processors that were invoked as part of the processing of this stack's event.
   *
   * @param processorPath the path to mark as invoked.
   * @throws EmptyStackException if this stack is empty.
   */
  public void setCurrentProcessorPath(String processorPath) {
    if (!innerStack.isEmpty()) {
      innerStack.push(new FlowStackElement(innerStack.pop().getFlowName(), processorPath));
    }
  }

  /**
   * Removes the top-most element from this stack.
   *
   * @return the top-most element of this stack.
   * @throws EmptyStackException if this stack is empty.
   */
  public FlowStackElement pop() {
    return innerStack.pop();
  }

  /**
   * Retrieves, but does not remove, the top-most element from this stack.
   *
   * @return the top-most element of this stack.
   * @throws EmptyStackException if this stack is empty.
   */
  public FlowStackElement peek() {
    return innerStack.peek();
  }

  @Override
  public List<FlowStackElement> getElements() {
    return new ArrayList<>(innerStack);
  }

  @Override
  public DefaultFlowCallStack clone() {
    return new DefaultFlowCallStack(innerStack);
  }

  @Override
  public String toString() {
    StringBuilder stackString = new StringBuilder();

    int i = 0;
    for (FlowStackElement flowStackElement : innerStack) {
      stackString.append("at ").append(flowStackElement.toString());
      if (++i != innerStack.size()) {
        stackString.append(lineSeparator());
      }
    }
    return stackString.toString();
  }
}
