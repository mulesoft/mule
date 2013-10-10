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
import org.mule.api.processor.MessageProcessor;

/**
 * <code>AsyncMessageNotification</code> when async work is scheduled and completed for a given flow
 */
public class AsyncMessageNotification extends ServerNotification implements BlockingServerEvent
{

    private static final long serialVersionUID = 6065691696506216248L;

    public static final int PROCESS_ASYNC_SCHEDULED = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 1;
    public static final int PROCESS_ASYNC_COMPLETE = ASYNC_MESSAGE_EVENT_ACTION_START_RANGE + 2;

    static
    {
        registerAction("async process scheduled", PROCESS_ASYNC_SCHEDULED);
        registerAction("async process complete", PROCESS_ASYNC_COMPLETE);
    }

    protected MessageProcessor messageProcessor;
    protected MessagingException exception;

    public AsyncMessageNotification(Pipeline pipeline,
                                    MuleEvent event,
                                    MessageProcessor messageProcessor,
                                    int action)
    {
        super(event, action, pipeline.getName());
        this.messageProcessor = messageProcessor;
    }

    public AsyncMessageNotification(Pipeline pipeline,
                                    MuleEvent event,
                                    MessageProcessor messageProcessor,
                                    int action,
                                    MessagingException exception)
    {
        this(pipeline, event, messageProcessor, action);
        this.exception = exception;
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }

    public MessagingException getException()
    {
        return exception;
    }
}
