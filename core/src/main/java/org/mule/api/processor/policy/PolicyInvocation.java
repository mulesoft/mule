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

    /**
     * Proceed using the current event.
     *
     * @see #setEvent(org.mule.api.MuleEvent)
     */
    public MuleEvent proceed() throws MuleException
    {
        return messageProcessor.process(event);
    }

    public MuleEvent getEvent()
    {
        return event;
    }

    /**
     * Replace the event object completely. Note that most of the time it's enough to simply
     * modify the event without any rewriting.
     *
     * @see #getEvent()
     */
    public void setEvent(MuleEvent event)
    {
        this.event = event;
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }

    /**
     * Set the message processor to be invoked on {@link #proceed()}. This may potentially disrupt the
     * execution chain, use wisely.
     */
    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }
}
