/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

/**
 * Intercepts a MessageProcessor execution.
 */
public interface MessageProcessorExecutionInterceptor
{
    public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException;
}
