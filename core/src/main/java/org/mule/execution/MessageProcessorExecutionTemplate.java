/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.processor.MessageProcessor;

/**
 * Template for executing a MessageProcessor.
 *
 * Provides all a set of functionality common to all MessageProcessor execution.
 */
public class MessageProcessorExecutionTemplate
{
    private MessageProcessorExecutionInterceptor executionInterceptor;

    private MessageProcessorExecutionTemplate(MessageProcessorExecutionInterceptor executionInterceptor)
    {
        this.executionInterceptor = executionInterceptor;
    }
    
    public static MessageProcessorExecutionTemplate createExceptionTransformerExecutionTemplate()
    {
        return new MessageProcessorExecutionTemplate(new ExceptionToMessagingExceptionExecutionInterceptor());
    }

    public static MessageProcessorExecutionTemplate createExecutionTemplate()
    {
        return new MessageProcessorExecutionTemplate(new MessageProcessorNotificationExecutionInterceptor(new ExceptionToMessagingExceptionExecutionInterceptor()));
    }

    public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException
    {
        return this.executionInterceptor.execute(messageProcessor,event);
    }
}
