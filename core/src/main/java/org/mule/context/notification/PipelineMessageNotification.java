/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.MuleEvent;
import org.mule.api.construct.Pipeline;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;

/**
 * <code>FlowConstructMessageNotification</code> is fired when a flow construct is executed.
 */
public class PipelineMessageNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 6065691696506216248L;

    // public static final int REQUEST_RECEIVED = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 1;
    public static final int PROCESS_BEGIN = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 2;
    public static final int PROCESS_REQUEST_END = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 3;
    public static final int PROCESS_RESPONSE_END = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 4;
    public static final int PROCESS_END = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 5;
    public static final int PROCESS_EXCEPTION = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 6;
    // public static final int RESPONSE_SENT = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 7;

    static
    {
        // registerAction("pipeline request message recieved", REQUEST_RECEIVED);
        registerAction("pipeline process begin", PROCESS_BEGIN);
        registerAction("pipeline request message processing complete", PROCESS_REQUEST_END);
        registerAction("pipeline response message processing complete", PROCESS_RESPONSE_END);
        registerAction("pipeline process end", PROCESS_END);
        registerAction("pipeline exception when processing", PROCESS_EXCEPTION);
        // registerAction("pipeline response message sent", RESPONSE_SENT);
    }

    public PipelineMessageNotification(Pipeline pipeline, MuleEvent event, int action)
    {
        super(event, action, pipeline.getName());
    }

}
