/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;

import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;

/**
 * Listener for PipelineMessageNotification that delegates notifications to NotificationTextDebugger
 */
public class FlowNotificationTextDebugger implements PipelineMessageNotificationListener<PipelineMessageNotification> {

  private final MessageProcessingFlowTraceManager messageProcessingFlowTraceManager;

  public FlowNotificationTextDebugger(MessageProcessingFlowTraceManager messageProcessingFlowTraceManager) {
    this.messageProcessingFlowTraceManager = messageProcessingFlowTraceManager;
  }

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public void onNotification(PipelineMessageNotification notification) {
    if (notification.getAction().getActionId() == PROCESS_COMPLETE) {
      messageProcessingFlowTraceManager.onPipelineNotificationComplete(notification);
    } else if (notification.getAction().getActionId() == PROCESS_START) {
      messageProcessingFlowTraceManager.onPipelineNotificationStart(notification);
    }
  }


}
