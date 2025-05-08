/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_FLOW_STACK_MAX_DEPTH;

import static java.lang.Integer.getInteger;
import static java.lang.System.lineSeparator;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.internal.event.EventContextDeepNestingException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import javax.xml.namespace.QName;

/**
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack {

  private static final long serialVersionUID = -8683711977929802819L;

  // BaseEventContext.class.getName() is here for backwards compatibility, since it was the equivalent property until 4.2.x
  private static final int MAX_DEPTH =
      getInteger(MULE_FLOW_STACK_MAX_DEPTH, getInteger(BaseEventContext.class.getName() + ".maxDepth", 45));

  private final Deque<FlowStackElement> innerStack;

  // The no-arg constructor is made public to prevent an issue with Kryo generated access classes and the module system.
  public static DefaultFlowCallStack newDefaultFlowCallStack() {
    return new DefaultFlowCallStack();
  }

  private DefaultFlowCallStack() {
    this.innerStack = new ArrayDeque<>(4);
  }

  private DefaultFlowCallStack(final Deque<FlowStackElement> innerStack) {
    this.innerStack = ((ArrayDeque<FlowStackElement>) innerStack).clone();
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
   */
  public void pushCurrentProcessorPath(String processorPath,
                                       ComponentLocation location, Map<QName, Object> annotations) {
    if (!innerStack.isEmpty()) {
      synchronized (innerStack) {
        FlowStackElement stackElement = innerStack.pop();
        innerStack.push(new FlowStackElement(stackElement.getFlowName(), stackElement.getChainIdentifier(), processorPath,
                                             location, annotations));
      }
    }
  }

  /**
   * Removes the top-most element from this stack.
   *
   * @return the top-most element of this stack.
   * @throws NoSuchElementException if this stack is empty.
   */
  public FlowStackElement pop() {
    synchronized (innerStack) {
      return innerStack.pop();
    }
  }

  /**
   * Retrieves, but does not remove, the top-most element from this stack.
   *
   * @return the top-most element of this stack, or null if this stack is empty.
   */
  @Override
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

  /**
   * Same as {@link #toString()} but including the milliseconds elapsed between its creation and now
   * ({@link FlowStackElement#getElapsedTimeLong()}.
   */
  @Override
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
