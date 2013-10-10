/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor.policy;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class PolicyInvocation
{
    private MuleEvent event;
    private List<AroundPolicy> policies = new LinkedList<AroundPolicy>();
    private volatile int currentPolicyIndex = 0;
    private MessageProcessor messageProcessor;

    public PolicyInvocation(MuleEvent event, List<AroundPolicy> policies, MessageProcessor processor)
    {
        this.event = event;
        this.policies = policies;
        this.messageProcessor = processor;
    }

    /**
     * Proceed using the current event.
     *
     * @see #setEvent(org.mule.api.MuleEvent)
     */
    public MuleEvent proceed() throws MuleException
    {
        currentPolicyIndex++;
        if (currentPolicyIndex == policies.size())
        {
            // end of chain
            return messageProcessor.process(event);
        }
        final AroundPolicy currentPolicy = getCurrentPolicy();
        final MuleEvent result = currentPolicy.invoke(this);
        setEvent(result);
        return event;
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

    /**
     * @return policy at the current index in the list
     */
    public AroundPolicy getCurrentPolicy()
    {
        return policies.get(currentPolicyIndex);
    }

    public MessageProcessor getMessageProcessor()
    {
        return messageProcessor;
    }

    public List<AroundPolicy> getPolicies()
    {
        return policies;
    }
}
