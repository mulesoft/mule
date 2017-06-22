/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack {

  private static final long serialVersionUID = -8683711977929802819L;

  private Stack<FlowStackElement> innerStack = new Stack<>();

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
    FlowStackElement topElement = innerStack.pop();
    innerStack.push(new FlowStackElement(topElement.getFlowName(), processorPath));
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

  @Override
  public List<FlowStackElement> getElements() {
    List<FlowStackElement> elementsCloned = new ArrayList<>();
    for (int i = innerStack.size() - 1; i >= 0; --i) {
      elementsCloned.add(innerStack.get(i));
    }
    return elementsCloned;
  }

  @Override
  public DefaultFlowCallStack clone() {
    DefaultFlowCallStack cloned = new DefaultFlowCallStack();
    for (int i = 0; i < innerStack.size(); ++i) {
      cloned.innerStack.push(innerStack.get(i));
    }

    return cloned;
  }

  @Override
  public String toString() {
    StringBuilder stackString = new StringBuilder();
    for (int i = innerStack.size() - 1; i >= 0; --i) {
      stackString.append("at ").append(innerStack.get(i).toString());
      if (i != 0) {
        stackString.append(System.lineSeparator());
      }
    }
    return stackString.toString();
  }
}
