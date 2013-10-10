/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.construct.Pipeline;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;

/**
 * <code>PipelineMessageNotification</code> is fired at key steps in the processing of {@link Pipeline}
 */
public class PipelineMessageNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 6065691696506216248L;

    // Fired when processing of pipeline starts
    public static final int PROCESS_START = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 1;
    // Fired when pipeline processing reaches the end before returning
    public static final int PROCESS_END = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 2;
    // Fired when pipeline processing returns after processing request and response message
    public static final int PROCESS_COMPLETE = PIPELINE_MESSAGE_EVENT_ACTION_START_RANGE + 4;

    static
    {
        registerAction("pipeline process start", PROCESS_START);
        registerAction("pipeline request message processing end", PROCESS_END);
        registerAction("pipeline process complete", PROCESS_COMPLETE);
    }

    protected MessagingException exception;

    public PipelineMessageNotification(Pipeline pipeline, MuleEvent event, int action)
    {
        super(event, action, pipeline.getName());
    }

    public PipelineMessageNotification(Pipeline pipeline,
                                       MuleEvent event,
                                       int action,
                                       MessagingException exception)
    {
        this(pipeline, event, action);
        this.exception = exception;
    }

    public MessagingException getException()
    {
        return exception;
    }

}
