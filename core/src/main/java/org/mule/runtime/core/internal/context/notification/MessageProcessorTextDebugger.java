/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
