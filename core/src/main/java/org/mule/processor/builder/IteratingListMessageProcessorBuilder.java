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
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.NullMessageProcessor;

import java.util.Collection;

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
public class IteratingListMessageProcessorBuilder extends AbstractCompositeMessageProcessorBuilder
{

    public MessageProcessorChain build() throws MuleException
    {
        if (processors.isEmpty())
        {
            return new NullMessageProcessor();
        }
        return new IteratingListCompositeMessageProcessor(name, processors);
    }

    public IteratingListMessageProcessorBuilder add(MessageProcessor... processors)
    {
        for (MessageProcessor messageProcessor : processors)
        {
            this.processors.add(messageProcessor);
        }
        return this;
    }

    public IteratingListMessageProcessorBuilder add(MessageProcessorBuilder... builders)
    {
        for (MessageProcessorBuilder builder : builders)
        {
            this.processors.add(builder);
        }
        return this;
    }

    public IteratingListMessageProcessorBuilder InterceptingChainMessageProcessorBuilder(Collection<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.processors.addAll(processors);
        }
        return this;
    }

    public IteratingListMessageProcessorBuilder addBefore(MessageProcessor processor)
    {
        this.processors.add(0, processor);
        return this;
    }

    public IteratingListMessageProcessorBuilder addBefore(MessageProcessorBuilder builder)
    {
        this.processors.add(0, builder);
        return this;
    }

}
