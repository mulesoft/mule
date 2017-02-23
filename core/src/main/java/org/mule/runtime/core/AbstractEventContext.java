/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Collections.unmodifiableList;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for implementations of {@link EventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements EventContext {

  private final List<EventContext> childContexts = new LinkedList<>();
  private boolean completed = false;
  private boolean streaming = false;

  @Override
  public List<EventContext> getChildContexts() {
    return unmodifiableList(childContexts);
  }

  void addChildContext(EventContext childContext) {
    childContexts.add(childContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success() {
    completed = true;
    doSuccess();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(Event event) {
    completed = true;
    doSuccess(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void error(Throwable throwable) {
    completed = true;
    doError(throwable);
  }

  /**
   * Template method to support the {@link #success()} method
   */
  protected abstract void doSuccess();

  /**
   * Template method to support the {@link #success(Event)} method
   */
  protected abstract void doSuccess(Event event);

  /**
   * Template method to support the {@link #error(Throwable)} method
   */
  protected abstract void doError(Throwable throwable);

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTerminated() {
    return completed ? childContexts.stream().allMatch(EventContext::isTerminated) : false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void streaming() {
    streaming = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isStreaming() {
    if (streaming) {
      return true;
    }

    return childContexts.isEmpty() ? false : childContexts.stream().anyMatch(EventContext::isStreaming);
  }
}
