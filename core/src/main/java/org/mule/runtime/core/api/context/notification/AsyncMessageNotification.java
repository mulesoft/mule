/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

/**
 * <code>AsyncMessageNotification</code> when async work is scheduled and completed for a given flow
 */
public class AsyncMessageNotification extends EnrichedServerNotification implements SynchronousServerEvent {

  private static final long serialVersionUID = 6065691696506216248L;

  public static final int PROCESS_ASYNC_SCHEDULED = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 1;
  public static final int PROCESS_ASYNC_COMPLETE = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 2;

  static {
    registerAction("async process scheduled", PROCESS_ASYNC_SCHEDULED);
    registerAction("async process complete", PROCESS_ASYNC_COMPLETE);
  }

  public AsyncMessageNotification(EnrichedNotificationInfo notificationInfo, FlowConstruct flowConstruct, int action) {
    super(notificationInfo, action, flowConstruct);
  }

  public Processor getMessageProcessor() {
    return (Processor) super.getComponent();
  }

  public MessagingException getException() {
    return (MessagingException) super.getException();
  }
}
