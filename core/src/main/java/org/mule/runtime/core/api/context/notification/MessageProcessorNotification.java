/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;

public class MessageProcessorNotification extends EnrichedServerNotification {

  private static final long serialVersionUID = 1L;

  public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
  public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
    registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
  }

  private InternalEventContext eventContext;

  public MessageProcessorNotification(EnrichedNotificationInfo notificationInfo, ComponentLocation componentLocation,
                                      InternalEventContext eventContext,
                                      int action) {
    super(notificationInfo, action, componentLocation != null ? componentLocation.getRootContainerName() : null);
    this.eventContext = eventContext;
  }

  public static MessageProcessorNotification createFrom(InternalEvent event, ComponentLocation componentLocation,
                                                        AnnotatedObject processor, MessagingException exceptionThrown,
                                                        int action) {
    EnrichedNotificationInfo notificationInfo = createInfo(event, exceptionThrown, processor);
    return new MessageProcessorNotification(notificationInfo, componentLocation, event.getContext(), action);
  }

  public Processor getProcessor() {
    return (Processor) getComponent();
  }

  public InternalEventContext getEventContext() {
    return eventContext;
  }

  @Override
  public MessagingException getException() {
    return (MessagingException) super.getException();
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", processor=" + getComponent().getLocation().getLocation()
        + ", resourceId="
        + resourceIdentifier + ", serverId=" + serverId + ", timestamp=" + timestamp + "}";
  }

  @Override
  public boolean isSynchronous() {
    return true;
  }
}
