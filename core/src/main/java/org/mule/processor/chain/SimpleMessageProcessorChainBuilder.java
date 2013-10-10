/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.chain;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;

import java.util.LinkedList;

public class SimpleMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder
{

    public SimpleMessageProcessorChainBuilder()
    {
        // empty
    }

    public SimpleMessageProcessorChainBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    protected DefaultMessageProcessorChain createInnerChain(LinkedList<MessageProcessor> tempList)
    {
        return new SimpleMessageProcessorChain(tempList);
    }

    @Override
    protected DefaultMessageProcessorChain createOuterChain(LinkedList<MessageProcessor> tempList)
    {
        return new SimpleMessageProcessorChain(tempList);
    }

}
