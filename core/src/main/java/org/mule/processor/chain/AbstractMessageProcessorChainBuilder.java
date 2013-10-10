/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.chain;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;

import java.util.ArrayList;
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
public abstract class AbstractMessageProcessorChainBuilder implements MessageProcessorChainBuilder
{

    protected List processors = new ArrayList();
    protected String name;
    protected FlowConstruct flowConstruct;

    public AbstractMessageProcessorChainBuilder()
    {
        // empty
    }

    public AbstractMessageProcessorChainBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    // Argument is of type Object because it could be a MessageProcessor or a MessageProcessorBuilder
    protected MessageProcessor initializeMessageProcessor(Object processor) throws MuleException
    {
        // TODO DF: FlowConstuct should be injected here but there is an issue with spring not have reference
        // to it. For now we inject it once the MessageProcessor is built and this works, but
        // MessageProcessorBuilders should have FlowConstuct available when building really.

        if (processor instanceof MessageProcessorBuilder)
        {
            return ((MessageProcessorBuilder) processor).build();
        }
        else
        {
            return (MessageProcessor) processor;
        }
    }

    public void setName(String name)
    {
        this.name = name;
    }

}
