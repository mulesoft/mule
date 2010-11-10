/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.chain;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.policy.Policies;

import java.util.List;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the
 * chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the nested
 * chain.
 */
public class InterceptingChainLifecycleWrapper implements MessageProcessorChain
{
    protected List<MessageProcessor> processors;
    private MessageProcessorChain chain;

    public InterceptingChainLifecycleWrapper(MessageProcessorChain chain, List<MessageProcessor> processors)
    {
        this.processors = processors;
        this.chain = chain;
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return chain.getMessageProcessors();
    }

    public String getName()
    {
        return chain.getName();
    }

    public Policies getPolicies()
    {
        return chain.getPolicies();
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return chain.process(event);
    }

    @Override
    public String toString()
    {
        return chain.toString();
    }

}
