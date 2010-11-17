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
import org.mule.routing.WireTap;

import java.util.List;
import java.util.Map;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the
 * chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the nested
 * chain.
 */
public class InterceptingChainLifecycleWrapper extends AbstractMessageProcessorChain
{
    private MessageProcessorChain chain;

    public InterceptingChainLifecycleWrapper(MessageProcessorChain chain,
                                             List<MessageProcessor> processors,
                                             String name)
    {
        super(name, processors);
        this.chain = chain;
    }

    @Override
    public String getName()
    {
        return chain.getName();
    }

    @Override
    public Policies getPolicies()
    {
        return chain.getPolicies();
    }

    @Override
    public Map<MessageProcessor, WireTap> getCallbackMap()
    {
        return chain.getCallbackMap();
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        // TODO something odd here, 'this' MP set is different from the delegate's set
        return chain.getMessageProcessors();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return chain.process(event);
    }

}
