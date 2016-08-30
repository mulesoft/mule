/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.BlockingServerEvent;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.processor.MessageProcessor;

/**
 * <code>AsyncMessageNotification</code> when async work is scheduled and completed for a given flow
 */
public class AsyncMessageNotification extends ServerNotification implements BlockingServerEvent {

  private static final long serialVersionUID = 6065691696506216248L;

  public static final int PROCESS_ASYNC_SCHEDULED = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 1;
  public static final int PROCESS_ASYNC_COMPLETE = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("async process scheduled", PROCESS_ASYNC_SCHEDULED);
    registerAction("async process complete", PROCESS_ASYNC_COMPLETE);
  }

  protected MessageProcessor messageProcessor;
  protected MessagingException exception;

  public AsyncMessageNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor messageProcessor, int action) {
    super(event, action, flowConstruct.getName());
    this.messageProcessor = messageProcessor;
  }

  public AsyncMessageNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor messageProcessor, int action,
                                  MessagingException exception) {
    this(flowConstruct, event, messageProcessor, action);
    this.exception = exception;
  }

  public MessageProcessor getMessageProcessor() {
    return messageProcessor;
  }

  public MessagingException getException() {
    return exception;
  }
}
