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
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor
{

    @Override
    public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException
    {
        try
        {
            return messageProcessor.process(event);
        }
        catch (MessagingException messagingException)
        {
            throw messagingException;
        }
        catch (Exception ex)
        {
            throw new MessagingException(event,ex,messageProcessor);
        }
    }
}
