/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Map;
import java.util.Optional;

/**
 * This class contains information about an event/exception, to be used in notifications without directly exposing it.
 */
public class EnrichedNotificationInfo {

  private String id;
  private String correlationId;
  private Message message;
  private Optional<Error> error;
  private org.mule.runtime.api.event.Event event;
  private Object component; // this should be AnnotatedObject, but not all interfaces mention it (though implementations do)
  private Exception exception;
  private Map<String, TypedValue> variables;
  private String originatingFlowName;
  private FlowCallStack flowCallStack;

  // TODO: MULE-12626: remove when Studio uses interception API
  Event internalEvent;

  /**
   * Extract information from the event and exception to provide notification data.
   *
   * @param event the event to extract information from
   * @param e the exception that occurred
   * @param component the component (processor, source, etc) that triggered the notification
   * @return
   */
  public static EnrichedNotificationInfo createInfo(Event event, Exception e, Object component) {
    EnrichedNotificationInfo notificationInfo;

    if (event != null) {
      if (component == null && e != null) {
        component = componentFromException(e);
      }

      notificationInfo =
          new EnrichedNotificationInfo(event, component, e, event.getFlowCallStack());
      notificationInfo.event = event;
      return notificationInfo;
    } else if (e != null) {
      if (e instanceof MessagingException) {
        MessagingException messagingException = (MessagingException) e;
        if (messagingException.getEvent() != null) {
          return createInfo(messagingException.getEvent(), e, componentFromException(e));
        }
      } else {
        notificationInfo = new EnrichedNotificationInfo(null, e, null, null);
        notificationInfo.event = event;
        return notificationInfo;
      }
    }

    throw new RuntimeException("Neither event or exception present");
  }

  private static AnnotatedObject componentFromException(Exception e) {
    if (e instanceof MessagingException) {
      MessagingException messagingException = (MessagingException) e;
      Processor messageProcessor = messagingException.getFailingMessageProcessor();

      if (messageProcessor instanceof AnnotatedObject) {
        return (AnnotatedObject) messageProcessor;
      }
    }
    return null;
  }

  public EnrichedNotificationInfo(Event event, Object component, Exception exception, FlowCallStack flowCallStack) {
    this.event = event;
    this.component = component;
    this.exception = exception;
    this.flowCallStack = flowCallStack;
  }

  public org.mule.runtime.api.event.Event getEvent() {
    return event;
  }

  public AnnotatedObject getComponent() {
    if (component != null && component instanceof AnnotatedObject) {
      return (AnnotatedObject) component;
    } else {
      return null;
    }
  }

  public Exception getException() {
    return exception;
  }

  public FlowCallStack getFlowCallStack() {
    return flowCallStack;
  }

}
