/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.core.api.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;

/**
 * Intercepts MessageProcessor execution to fire before and after notifications
 */
class MessageProcessorNotificationExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private MessageProcessorExecutionInterceptor next;
  private MuleContext muleContext;
  private FlowConstruct flowConstruct;

  MessageProcessorNotificationExecutionInterceptor(MessageProcessorExecutionInterceptor next) {
    this.next = next;
  }

  MessageProcessorNotificationExecutionInterceptor() {

  }


  @Override
  public Event execute(final Processor messageProcessor, final Event event) throws MessagingException {
    final ServerNotificationManager notificationManager = muleContext.getNotificationManager();
    final boolean fireNotification = event.isNotificationsEnabled();
    if (fireNotification) {
      fireNotification(notificationManager, flowConstruct, event, messageProcessor, null, MESSAGE_PROCESSOR_PRE_INVOKE);
    }

    Event eventToProcess = event;
    Event result = null;
    MessagingException exceptionThrown = null;

    // Update RequestContext ThreadLocal in case if previous processor modified it
    // also for backwards compatibility
    setCurrentEvent(eventToProcess);

    try {
      if (next == null) {
        result = messageProcessor.process(eventToProcess);
      } else {
        result = next.execute(messageProcessor, eventToProcess);
      }
    } catch (MessagingException e) {
      exceptionThrown = e;
      throw e;
    } catch (MuleException e) {
      exceptionThrown = new MessagingException(event, e, messageProcessor);
      throw exceptionThrown;
    } finally {
      if (fireNotification) {
        fireNotification(notificationManager, flowConstruct, result != null ? result : event, messageProcessor, exceptionThrown,
                         MESSAGE_PROCESSOR_POST_INVOKE);
      }
    }
    return result;
  }

  public static void fireNotification(ServerNotificationManager serverNotificationManager, FlowConstruct flowConstruct,
                                      Event event, Processor processor, MessagingException exceptionThrown, int action) {
    if (serverNotificationManager != null
        && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)) {
      if ((processor instanceof AnnotatedObject) && ((AnnotatedObject) processor).getAnnotation(LOCATION_KEY) != null) {
        serverNotificationManager
            .fireNotification(MessageProcessorNotification.createFrom(event, flowConstruct, processor, exceptionThrown, action));
      }
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (next != null) {
      next.setMuleContext(context);
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
    if (next != null) {
      next.setFlowConstruct(flowConstruct);
    }
  }
}
