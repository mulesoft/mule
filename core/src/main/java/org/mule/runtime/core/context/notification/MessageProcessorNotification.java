/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.MessageProcessorPathResolver;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.ObjectUtils;

public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent {

  private static final long serialVersionUID = 1L;

  public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
  public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

  private final transient Processor processor;
  private final transient FlowConstruct flowConstruct;
  private final String processorPath;

  static {
    registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
    registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
  }

  private static ThreadLocal<String> lastRootMessageId = new ThreadLocal<>();
  private MessagingException exceptionThrown;


  public MessageProcessorNotification(FlowConstruct flowConstruct, Event event, Processor processor,
                                      MessagingException exceptionThrown, int action) {
    super(produceEvent(event, flowConstruct), action, flowConstruct.getName());
    this.exceptionThrown = exceptionThrown;
    this.processor = processor;

    if (flowConstruct instanceof MessageProcessorPathResolver) {
      this.processorPath = ((MessageProcessorPathResolver) flowConstruct).getProcessorPath(processor);
    } else {
      this.processorPath = null;
    }

    this.flowConstruct = flowConstruct;
  }

  @Override
  public Event getSource() {
    if (source instanceof String) {
      return null;
    }
    return (Event) super.getSource();
  }

  public Processor getProcessor() {
    return processor;
  }

  protected String processorToString() {
    if (processor == null) {
      return "";
    }

    String name;
    if (processor instanceof NameableObject) {
      name = String.format("%s '%s'", processor.getClass().getName(), ((NameableObject) processor).getName());
    } else {
      name = ObjectUtils.identityToString(processor);
    }
    return name;
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
      final InternalMessage msg = InternalMessage.builder().nullPayload().build();
      return Event.builder(create(flowConstruct, "MessageProcessorNotification", lastRootMessageId.get())).message(msg)
          .flow(flowConstruct).exchangePattern(REQUEST_RESPONSE).build();
    } else {
      return null;
    }
  }

  public MessagingException getExceptionThrown() {
    return exceptionThrown;
  }

  public String getProcessorPath() {
    return processorPath;
  }

  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", processor=" + processorPath + ", resourceId="
        + resourceIdentifier + ", serverId=" + serverId + ", timestamp=" + timestamp + "}";
  }

}
