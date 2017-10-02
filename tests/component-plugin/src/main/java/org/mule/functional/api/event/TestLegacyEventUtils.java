/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.event;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.lang.reflect.Field;

/**
 * Test utilities to access encapsulated fields and behavior from {@link CoreEvent}s.
 *
 * @since 4.0
 */
public final class TestLegacyEventUtils {

  private TestLegacyEventUtils() {
    // Nothing to do
  }

  public static final FlowExceptionHandler HANDLER = NullExceptionHandler.getInstance();

  /**
   * Events have a list of message processor paths it went trough so that the execution path of an event can be reconstructed
   * after it has executed.
   *
   * @return the message processors trace associated to the given event.
   */
  public static ProcessorsTrace getProcessorsTrace(CoreEvent event) {
    return ((BaseEventContext) event.getContext()).getProcessorsTrace();
  }

  /**
   * @return the {@link FlowExceptionHandler} to be applied if an exception is unhandled during the processing of the given
   *         event.
   */
  public static FlowExceptionHandler getEffectiveExceptionHandler(CoreEvent event) {
    Field exceptionHandlerField;
    try {
      exceptionHandlerField = event.getContext().getClass().getSuperclass().getDeclaredField("exceptionHandler");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    exceptionHandlerField.setAccessible(true);
    try {
      BaseEventContext eventContext = (BaseEventContext) event.getContext();

      FlowExceptionHandler effectiveMessagingExceptionHandler =
          (FlowExceptionHandler) exceptionHandlerField.get(eventContext);
      while (eventContext.getParentContext().isPresent() && effectiveMessagingExceptionHandler == HANDLER) {
        eventContext = eventContext.getParentContext().get();
        effectiveMessagingExceptionHandler = (FlowExceptionHandler) exceptionHandlerField.get(eventContext);
      }

      return effectiveMessagingExceptionHandler;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      exceptionHandlerField.setAccessible(false);
    }
  }

  public static Object getSessionProperty(CoreEvent event, String property) {
    return ((PrivilegedEvent) event).getSession().getProperty(property);
  }

  public static DataType getSessionPropertyDataType(CoreEvent event, String property) {
    return ((PrivilegedEvent) event).getSession().getPropertyDataType(property);
  }

  public static Object removeSessionProperty(CoreEvent event, String property) {
    return ((PrivilegedEvent) event).getSession().removeProperty(property);
  }

  /**
   * Return the event associated with the currently executing thread.
   *
   * @return event for currently executing thread.
   */
  public static CoreEvent getCurrentEvent() {
    return PrivilegedEvent.getCurrentEvent();
  }
}
