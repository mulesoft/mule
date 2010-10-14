/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.builder;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.chain.DefaultMessageProcessorChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Constructs a chain of {@link MessageProcessor}s and wraps the invocation of the chain in a composite
 * MessageProcessor. Both MessageProcessors and InterceptingMessageProcessor's can be chained together
 * arbitrarily in a single chain. InterceptingMessageProcessors simply intercept the next MessageProcessor in
 * the chain. When other non-intercepting MessageProcessors are used an adapter is used internally to chain
 * the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
public class InterceptingChainMessageProcessorBuilder extends AbstractCompositeMessageProcessorBuilder
{

    public InterceptingChainMessageProcessorBuilder()
    {
        // empty
    }

    public InterceptingChainMessageProcessorBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public MessageProcessorChain build() throws MuleException
    {
        LinkedList<MessageProcessor> tempList = new LinkedList<MessageProcessor>();

        // Start from last but one message processor and work backwards
        for (int i = processors.size() - 1; i >= 0; i--)
        {
            MessageProcessor processor = initializeMessageProcessor(processors.get(i));
            if ((processors.get(i)) instanceof InterceptingMessageProcessor)
            {
                if (i + 1 < processors.size())
                {
                    if (tempList.isEmpty())
                    {
                        ((InterceptingMessageProcessor) processor).setListener(initializeMessageProcessor(processors.get(i + 1)));
                    }
                    else
                    {
                        ((InterceptingMessageProcessor) processor).setListener(new DefaultMessageProcessorChain(
                            new ArrayList<MessageProcessor>(tempList)));
                    }
                }
                tempList = new LinkedList<MessageProcessor>(Collections.singletonList(processor));
            }
            else
            {
                tempList.addFirst(initializeMessageProcessor(processor));
            }
        }
        return new InterceptingMessageProcessorChain(new DefaultMessageProcessorChain(
            new ArrayList<MessageProcessor>(tempList)), processors, "");
    }

    public InterceptingChainMessageProcessorBuilder chain(MessageProcessor... processors)
    {
        for (MessageProcessor messageProcessor : processors)
        {
            this.processors.add(messageProcessor);
        }
        return this;
    }

    public InterceptingChainMessageProcessorBuilder chain(List<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.processors.addAll(processors);
        }
        return this;
    }

    public InterceptingChainMessageProcessorBuilder chain(MessageProcessorBuilder... builders)
    {
        for (MessageProcessorBuilder messageProcessorBuilder : builders)
        {
            this.processors.add(messageProcessorBuilder);
        }
        return this;
    }

    public InterceptingChainMessageProcessorBuilder chainBefore(MessageProcessor processor)
    {
        this.processors.add(0, processor);
        return this;
    }

    public InterceptingChainMessageProcessorBuilder chainBefore(MessageProcessorBuilder builder)
    {
        this.processors.add(0, builder);
        return this;
    }

}
