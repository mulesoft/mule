/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
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

    public static MessageProcessorExecutionTemplate createNotificationExecutionTemplate()
    {
        return new MessageProcessorExecutionTemplate(new MessageProcessorNotificationExecutionInterceptor());
    }

    public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException
    {
        return this.executionInterceptor.execute(messageProcessor,event);
    }
}
