/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor.policy;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

/**
 *
 */
public class PolicyInvocation
{
    private MuleEvent event;
    private MessageProcessor messageProcessor;

    public PolicyInvocation(MuleEvent event, MessageProcessor messageProcessor)
    {
        this.event = event;
        this.messageProcessor = messageProcessor;
    }

    public MuleEvent proceed() throws MuleException
    {
        return messageProcessor.process(event);
    }

    public MuleEvent getEvent()
    {
        return event;
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }
}
