/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.component;

import static org.mule.runtime.core.api.util.ClassUtils.NO_ARGS;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.internal.exception.AbstractMessagingExceptionStrategy;

/**
 * Provides a way to define custom exception strategies on test flows.
 */
public class TestExceptionStrategy extends AbstractMessagingExceptionStrategy implements MessagingExceptionHandler {

  private final ExceptionStrategyCallback callback;

  /**
   * Creates a new exception strategy
   *
   * @param callbackClassName name of the callback's class to instantiate to process the thrown exceptions
   */
  public TestExceptionStrategy(String callbackClassName) {
    try {
      callback = (ExceptionStrategyCallback) instantiateClass(callbackClassName, NO_ARGS, getClass());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Event handleException(MessagingException ex, Event event) {
    return callback.handleException(ex, event,
                                    (exception, event1) -> TestExceptionStrategy.super.handleException(ex, event1));

  }

  public ExceptionStrategyCallback getCallback() {
    return callback;
  }
}
