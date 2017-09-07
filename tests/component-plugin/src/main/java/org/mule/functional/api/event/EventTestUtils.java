/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.event;

import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;

import java.lang.reflect.Field;

/**
 * Test utilities to access encapsulated fields and behavior from {@link BaseEvent}s.
 *
 * @since 4.0
 */
public final class EventTestUtils {

  private EventTestUtils() {
    // Nothing to do
  }

  public static final MessagingExceptionHandler HANDLER = NullExceptionHandler.getInstance();

  /**
   * Events have a list of message processor paths it went trough so that the execution path of an event can be reconstructed
   * after it has executed.
   *
   * @return the message processors trace associated to the given event.
   */
  public static ProcessorsTrace getProcessorsTrace(BaseEvent event) {
    return ((BaseEventContext) event.getContext()).getProcessorsTrace();
  }

  /**
   * @return the {@link MessagingExceptionHandler} to be applied if an exception is unhandled during the processing of the given
   *         event.
   */
  public static MessagingExceptionHandler getEffectiveExceptionHandler(BaseEvent event) {
    Field exceptionHandlerField;
    try {
      exceptionHandlerField = event.getContext().getClass().getSuperclass().getDeclaredField("exceptionHandler");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    exceptionHandlerField.setAccessible(true);
    try {
      BaseEventContext eventContext = (BaseEventContext) event.getContext();

      MessagingExceptionHandler effectiveMessagingExceptionHandler =
          (MessagingExceptionHandler) exceptionHandlerField.get(eventContext);
      while (eventContext.getParentContext().isPresent() && effectiveMessagingExceptionHandler == HANDLER) {
        eventContext = eventContext.getParentContext().get();
        effectiveMessagingExceptionHandler = (MessagingExceptionHandler) exceptionHandlerField.get(eventContext);
      }

      return effectiveMessagingExceptionHandler;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      exceptionHandlerField.setAccessible(false);
    }

  }
}
