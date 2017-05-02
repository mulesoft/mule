/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.GroupCorrelation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EnrichedNotificationInfo {

  private String id;
  private String correlationId;
  private GroupCorrelation groupCorrelation;
  private Message message;
  private Optional<Error> error;
  private Object component; // this should be AnnotatedObject, but not all interfaces mention it (though implementations do)
  private Exception exception;
  private Map<String, TypedValue> variables;
  private String originatingFlowName;
  private FlowCallStack flowCallStack;

  public EnrichedNotificationInfo(String uniqueId, String correlationId, GroupCorrelation groupCorrelation, Message message,
                                  Optional<Error> error, Object component, Exception exception,
                                  Map<String, TypedValue> variables, String originatingFlowName, FlowCallStack flowCallStack) {
    this.id = uniqueId;
    this.correlationId = correlationId;
    this.groupCorrelation = groupCorrelation;
    this.message = message;
    this.error = error;
    this.component = component;
    this.exception = exception;
    this.variables = variables;
    this.originatingFlowName = originatingFlowName;
    this.flowCallStack = flowCallStack;
  }

  public static EnrichedNotificationInfo createInfo(Event event, Exception e, Object component) {
    if (event != null) {
      if (component == null && e != null) {
        component = componentFromException(e);
      }

      return new EnrichedNotificationInfo(event.getContext().getId(), event.getCorrelationId(), event.getGroupCorrelation(),
                                          event.getMessage(), event.getError(), component, e, createVariablesMap(event),
                                          event.getContext().getOriginatingFlowName(), event.getFlowCallStack());
    } else if (e != null) {
      if (e instanceof MessagingException) {
        MessagingException messagingException = (MessagingException) e;
        if (messagingException.getEvent() != null) {
          return createInfo(messagingException.getEvent(), e, componentFromException(e));
        }
      } else {
        return new EnrichedNotificationInfo(null, null, null,
                                            null, null, null, e, new HashMap<>(), null, null);
      }
    }

    throw new RuntimeException("Neither event or exception present");
  }

  public static Map<String, TypedValue> createVariablesMap(Event event) {
    Map<String, TypedValue> variables = new HashMap<>();

    event.getVariableNames().forEach(name -> {
      variables.put(name, event.getVariable(name));
    });

    return variables;
  }

  public static AnnotatedObject componentFromException(Exception e) {
    if (e instanceof MessagingException) {
      MessagingException messagingException = (MessagingException) e;
      Processor messageProcessor = messagingException.getFailingMessageProcessor();

      if (messageProcessor instanceof AnnotatedObject) {
        return (AnnotatedObject) messageProcessor;
      }
    }
    return null;
  }

  public String getUniqueId() {
    return id;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public GroupCorrelation getGroupCorrelation() {
    return groupCorrelation;
  }

  public Message getMessage() {
    return message;
  }

  public Optional<Error> getError() {
    return error;
  }

  public Map<String, TypedValue> getVariables() {
    return variables;
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

  public String getOriginatingFlowName() {
    return originatingFlowName;
  }

  public FlowCallStack getFlowCallStack() {
    return flowCallStack;
  }
}
