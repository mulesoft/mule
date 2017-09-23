/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;

import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;

/**
 * Listener for MessageProcessorNotification that delegates notifications to NotificationTextDebugger
 */
public class MessageProcessorTextDebugger implements MessageProcessorNotificationListener<MessageProcessorNotification> {

  private final MessageProcessingFlowTraceManager messageProcessingFlowTraceManager;

  @Override
  public boolean isBlocking() {
    return false;
  }

  public MessageProcessorTextDebugger(MessageProcessingFlowTraceManager messageProcessingFlowTraceManager) {
    this.messageProcessingFlowTraceManager = messageProcessingFlowTraceManager;
  }

  @Override
  public void onNotification(MessageProcessorNotification notification) {
    if (notification.getAction().getActionId() == MESSAGE_PROCESSOR_PRE_INVOKE) {
      messageProcessingFlowTraceManager.onMessageProcessorNotificationPreInvoke(notification);
    }
  }

}
