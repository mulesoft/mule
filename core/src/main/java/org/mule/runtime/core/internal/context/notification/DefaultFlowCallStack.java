/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.lang.Integer.getInteger;
import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_FLOW_STACK_MAX_DEPTH;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.internal.event.EventContextDeepNestingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;
import java.util.function.Function;

/**
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack {

  private static final long serialVersionUID = -8683711977929802819L;

  // BaseEventContext.class.getName() is here for backwards compatibility, since it was the equivalent property until 4.2.x
  private static final int MAX_DEPTH =
      getInteger(MULE_FLOW_STACK_MAX_DEPTH, getInteger(BaseEventContext.class.getName() + ".maxDepth", 50));

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
    if (innerStack.size() >= MAX_DEPTH) {
      StringBuilder messageBuilder = new StringBuilder();

      messageBuilder.append("Too many nested child contexts.")
          .append(lineSeparator())
          .append(toString());

      throw new EventContextDeepNestingException(messageBuilder.toString());
    }

    synchronized (innerStack) {
      innerStack.push(flowStackElement);
    }
  }

  /**
   * Adds a message processor path to the list of processors that were invoked as part of the processing of this stack's event.
   *
   * @param processorPath the path to mark as invoked.
   * @throws EmptyStackException if this stack is empty.
   */
  public void setCurrentProcessorPath(String processorPath) {
    if (!innerStack.isEmpty()) {
      synchronized (innerStack) {
        innerStack.push(new FlowStackElement(innerStack.pop().getFlowName(), processorPath));
      }
    }
  }

  /**
   * Removes the top-most element from this stack.
   *
   * @return the top-most element of this stack.
   * @throws EmptyStackException if this stack is empty.
   */
  public FlowStackElement pop() {
    synchronized (innerStack) {
      return innerStack.pop();
    }
  }

  /**
   * Retrieves, but does not remove, the top-most element from this stack.
   *
   * @return the top-most element of this stack.
   * @throws EmptyStackException if this stack is empty.
   */
  public FlowStackElement peek() {
    synchronized (innerStack) {
      return innerStack.peek();
    }
  }

  @Override
  public List<FlowStackElement> getElements() {
    synchronized (innerStack) {
      return new ArrayList<>(innerStack);
    }
  }

  @Override
  public DefaultFlowCallStack clone() {
    synchronized (innerStack) {
      return new DefaultFlowCallStack(innerStack);
    }
  }

  @Override
  public String toString() {
    return doToString(FlowStackElement::toString);
  }

  public String toStringWithElapsedTime() {
    return doToString(FlowStackElement::toStringWithElapsedTime);
  }

  private String doToString(Function<FlowStackElement, String> toString) {
    StringBuilder stackString = new StringBuilder(256);

    int i = 0;
    synchronized (innerStack) {
      for (FlowStackElement flowStackElement : innerStack) {
        stackString.append("at ").append(toString.apply(flowStackElement));
        if (++i != innerStack.size()) {
          stackString.append(lineSeparator());
        }
      }
    }
    return stackString.toString();
  }
}
