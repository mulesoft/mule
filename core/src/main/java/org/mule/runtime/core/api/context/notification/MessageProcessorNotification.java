/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

public class MessageProcessorNotification extends EnrichedServerNotification implements SynchronousServerEvent {

  private static final long serialVersionUID = 1L;

  public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
  public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
    registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
  }

  private static ThreadLocal<String> lastRootMessageId = new ThreadLocal<>();
  private EventContext eventContext;

  public MessageProcessorNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct,
                                      EventContext eventContext, int action) {
    super(notificationInfo, action, flowConstruct);
    this.eventContext = eventContext;
  }

  public static MessageProcessorNotification createFrom(Event event, FlowConstruct flowConstruct, Processor processor,
                                                        MessagingException exceptionThrown, int action) {
    EnrichedNotificationInfo notificationInfo =
        createInfo(produceEvent(event, flowConstruct), exceptionThrown, processor);
    return new MessageProcessorNotification(notificationInfo, flowConstruct, event.getContext(), action);
  }

  public Processor getProcessor() {
    return (Processor) getComponent();
  }

  public EventContext getEventContext() {
    return eventContext;
  }

  /**
   * If event is null, produce and event with the proper message root ID, to allow it to be correlated with others in the thread
   */
  private static Event produceEvent(Event sourceEvent, FlowConstruct flowConstruct) {
    String rootId = lastRootMessageId.get();
    if (sourceEvent != null) {
      lastRootMessageId.set(sourceEvent.getCorrelationId());
      return sourceEvent;
    } else if (rootId != null && flowConstruct != null) {
      final Message msg = of(null);
      return Event.builder(create(flowConstruct, fromSingleComponent("MessageProcessorNotification"), lastRootMessageId.get()))
          .message(msg)
          .flow(flowConstruct).build();
    } else {
      return null;
    }
  }

  public MessagingException getException() {
    return (MessagingException) super.getException();
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", processor=" + getComponent().getLocation().getLocation()
        + ", resourceId="
        + resourceIdentifier + ", serverId=" + serverId + ", timestamp=" + timestamp + "}";
  }

  // TODO: MULE-12626: remove when Studio uses interception API
  public TypedValue evaluateExpression(ExpressionManager expressionManager, String script) {
    return getInfo().evaluateExpression(expressionManager, script);
  }
}
