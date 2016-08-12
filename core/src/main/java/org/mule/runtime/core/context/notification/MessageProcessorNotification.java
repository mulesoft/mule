/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.DefaultMessageExecutionContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.MessageProcessorPathResolver;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;

public class MessageProcessorNotification extends ServerNotification implements BlockingServerEvent {

  private static final long serialVersionUID = 1L;

  public static final int MESSAGE_PROCESSOR_PRE_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 1;
  public static final int MESSAGE_PROCESSOR_POST_INVOKE = MESSAGE_PROCESSOR_EVENT_ACTION_START_RANGE + 2;

  private final transient MessageProcessor processor;

  static {
    registerAction("message processor pre invoke", MESSAGE_PROCESSOR_PRE_INVOKE);
    registerAction("message processor post invoke", MESSAGE_PROCESSOR_POST_INVOKE);
  }

  private static ThreadLocal<String> lastRootMessageId = new ThreadLocal<>();
  private MessagingException exceptionThrown;

  public MessageProcessorNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor,
                                      MessagingException exceptionThrown, int action) {
    super(produceEvent(event, flowConstruct), action, flowConstruct != null ? flowConstruct.getName() : null);
    this.exceptionThrown = exceptionThrown;
    this.processor = processor;
  }

  @Override
  public MuleEvent getSource() {
    if (source instanceof String) {
      return null;
    }
    return (MuleEvent) super.getSource();
  }

  public MessageProcessor getProcessor() {
    return processor;
  }

  protected String toString(Object obj) {
    if (obj == null) {
      return "";
    }

    String name;
    if (obj instanceof NameableObject) {
      name = String.format("%s '%s'", obj.getClass().getName(), ((NameableObject) obj).getName());
    } else {
      name = ObjectUtils.identityToString(obj);
    }
    return name;
  }

  /**
   * If event is null, produce and event with the proper message root ID, to allow it to be correlated with others in the thread
   */
  private static MuleEvent produceEvent(MuleEvent sourceEvent, FlowConstruct flowConstruct) {
    String rootId = lastRootMessageId.get();
    if (sourceEvent != null && !VoidMuleEvent.getInstance().equals(sourceEvent)) {
      lastRootMessageId.set(sourceEvent.getMessage().getMessageRootId());
      return sourceEvent;
    } else if (rootId != null && flowConstruct != null) {
      final MuleMessage msg = MuleMessage.builder().nullPayload().rootId(rootId).build();

      return new DefaultMuleEvent(new DefaultMessageExecutionContext(flowConstruct.getMuleContext().getUniqueIdString(), null),
                                  msg, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct);
    } else {
      return null;
    }
  }

  public MessagingException getExceptionThrown() {
    return exceptionThrown;
  }

  public String getProcessorPath() {
    FlowConstruct fc = getSource().getFlowConstruct();
    if (!(fc instanceof MessageProcessorPathResolver)) {
      return null;
    }
    return ((MessageProcessorPathResolver) fc).getProcessorPath(processor);
  }
}
