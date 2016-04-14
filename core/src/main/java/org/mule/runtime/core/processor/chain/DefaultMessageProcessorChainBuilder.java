/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;

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
public class DefaultMessageProcessorChainBuilder extends AbstractMessageProcessorChainBuilder
{

    public DefaultMessageProcessorChainBuilder()
    {
        // empty
    }

    public DefaultMessageProcessorChainBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    /**
     * This builder supports the chaining together of message processors that intercept and also those that
     * don't. While one can iterate over message processor intercepting message processors need to be chained
     * together. One solution is make all message processors intercepting (via adaption) and chain them all
     * together, this results in huge stack traces and recursive calls with adaptor. The alternative is to
     * build the chain in such a way that we iterate when we can and chain where we need to. <br>
     * We iterate over the list of message processor to be chained together in reverse order collecting up
     * those that can be iterated over in a temporary list, as soon as we have an intercepting message
     * processor we create a DefaultMessageProcessorChain using the temporary list and set it as a listener of
     * the intercepting message processor and then we continue with the algorithm
     */
    public MessageProcessorChain build() throws MuleException
    {
        LinkedList<MessageProcessor> tempList = new LinkedList<MessageProcessor>();

        // Start from last but one message processor and work backwards
        for (int i = processors.size() - 1; i >= 0; i--)
        {
            MessageProcessor processor = initializeMessageProcessor(processors.get(i));
            if (processor instanceof InterceptingMessageProcessor)
            {
                InterceptingMessageProcessor interceptingProcessor = (InterceptingMessageProcessor) processor;
                // Processor is intercepting so we can't simply iterate
                if (i + 1 < processors.size())
                {
                    // The current processor is not the last in the list
                    if (tempList.isEmpty())
                    {
                        interceptingProcessor.setListener(initializeMessageProcessor(processors.get(i + 1)));
                    }
                    else if (tempList.size() == 1)
                    {
                        interceptingProcessor.setListener(tempList.get(0));
                    }
                    else
                    {
                        interceptingProcessor.setListener(createInnerChain(tempList));
                    }
                }
                tempList = new LinkedList<MessageProcessor>(Collections.singletonList(processor));
            }
            else
            {
                // Processor is not intercepting so we can invoke it using iteration
                // (add to temp list)
                tempList.addFirst(initializeMessageProcessor(processor));
            }
        }
        // Create the final chain using the current tempList after reserve iteration is complete. This temp
        // list contains the first n processors in the chain that are not intercepting.. with processor n+1
        // having been injected as the listener of processor n
        final DefaultMessageProcessorChain chain = createOuterChain(tempList);

        return buildMessageProcessorChain(chain);
    }

    protected MessageProcessorChain buildMessageProcessorChain(DefaultMessageProcessorChain chain)
    {
        // Wrap with something that can apply lifecycle to all processors which are otherwise not visable from
        // DefaultMessageProcessorChain
        return new InterceptingChainLifecycleWrapper(chain, processors, "wrapper for " + name);
    }

    protected DefaultMessageProcessorChain createInnerChain(LinkedList<MessageProcessor> tempList)
    {
        return new DefaultMessageProcessorChain("(inner iterating chain) of " + name,
            new ArrayList<MessageProcessor>(tempList));
    }

    protected DefaultMessageProcessorChain createOuterChain(LinkedList<MessageProcessor> tempList)
    {
        return new DefaultMessageProcessorChain("(inner iterating chain) of " + name,
            new ArrayList<MessageProcessor>(tempList));
    }

    public DefaultMessageProcessorChainBuilder chain(MessageProcessor... processors)
    {
        for (MessageProcessor messageProcessor : processors)
        {
            this.processors.add(messageProcessor);
        }
        return this;
    }

    public DefaultMessageProcessorChainBuilder chain(List<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.processors.addAll(processors);
        }
        return this;
    }

    public DefaultMessageProcessorChainBuilder chain(MessageProcessorBuilder... builders)
    {
        for (MessageProcessorBuilder messageProcessorBuilder : builders)
        {
            this.processors.add(messageProcessorBuilder);
        }
        return this;
    }

    public DefaultMessageProcessorChainBuilder chainBefore(MessageProcessor processor)
    {
        this.processors.add(0, processor);
        return this;
    }

    public DefaultMessageProcessorChainBuilder chainBefore(MessageProcessorBuilder builder)
    {
        this.processors.add(0, builder);
        return this;
    }
}
