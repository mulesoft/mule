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

import java.util.List;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the
 * chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the nested
 * chain.
 */
public class InterceptingMessageProcessorChain extends AbstractMessageProcessorChain
{
    private MessageProcessor firstInChain;
 
    public InterceptingMessageProcessorChain(MessageProcessor firstInChain,
                                             List<MessageProcessor> processors,
                                             String name)
    {
        super(name, processors);
        this.firstInChain = firstInChain;
    }

    public void setFirstInChain(MessageProcessor chain)
    {
        this.firstInChain = chain;
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return processors;
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return firstInChain.process(event);
    }

    public void setMessageProcessors(List processors)
    {
        this.processors = processors;
        
    }

}
