/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.PipelineMessageNotificationListener;

/**
 * Listener for PipelineMessageNotification that delegates notifications to NotificationTextDebugger
 */
public class FlowNotificationTextDebugger implements PipelineMessageNotificationListener<PipelineMessageNotification>
{

    private final MessageProcessingFlowTraceManager messageProcessingFlowTraceManager;

    public FlowNotificationTextDebugger(MessageProcessingFlowTraceManager messageProcessingFlowTraceManager)
    {
        this.messageProcessingFlowTraceManager = messageProcessingFlowTraceManager;
    }


    @Override
    public void onNotification(PipelineMessageNotification notification)
    {
        if (notification.getAction() == PipelineMessageNotification.PROCESS_COMPLETE)
        {
            messageProcessingFlowTraceManager.onPipelineNotificationComplete(notification);
        }
        else if (notification.getAction() == PipelineMessageNotification.PROCESS_START)
        {
            messageProcessingFlowTraceManager.onPipelineNotificationStart(notification);
        }
    }


}
