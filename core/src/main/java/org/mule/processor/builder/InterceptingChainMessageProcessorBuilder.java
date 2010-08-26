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

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.NullMessageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Constructs a chain of {@link MessageProcessor}s and wraps the invocation of the
 * chain in a composite MessageProcessor. Both MessageProcessors and
 * InterceptingMessageProcessor's can be chained together arbitrarily in a single
 * chain. InterceptingMessageProcessors simply intercept the next MessageProcessor in
 * the chain. When other non-intercepting MessageProcessors are used an adapter is
 * used internally to chain the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other
 * chains as required.
 * </p>
 */
public class InterceptingChainMessageProcessorBuilder implements MessageProcessorBuilder
{

    protected List processors = new ArrayList();
    protected String name;
    protected FlowConstruct flowConstruct;

    public InterceptingChainMessageProcessorBuilder()
    {
        // empty
    }
    
    public InterceptingChainMessageProcessorBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
    
    public MessageProcessor build() throws MuleException
    {
        if (processors.isEmpty())
        {
            return new NullMessageProcessor();
        }

        InterceptingMessageProcessor first = createInterceptingMessageProcessor(initializeMessageProcessor(processors.get(0)));
        MessageProcessor composite = new InterceptingChainCompositeMessageProcessor(first, processors, name);
        InterceptingMessageProcessor current = first;

        for (int i = 1; i < processors.size(); i++)
        {
            InterceptingMessageProcessor mp = createInterceptingMessageProcessor(initializeMessageProcessor(processors.get(i)));
            current.setListener(mp);
            current = mp;
        }
        return composite;
    }

    // Argument is of type Object because it could be a MessageProcessor or a MessageProcessorBuilder
    protected MessageProcessor initializeMessageProcessor(Object processor) throws MuleException
    {
        if (processor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
        }
        
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

    private InterceptingMessageProcessor createInterceptingMessageProcessor(MessageProcessor processor)
    {
        if (processor instanceof InterceptingMessageProcessor)
        {
            return (InterceptingMessageProcessor) processor;
        }
        else
        {
            return new InterceptingMessageProcessorAdapter(processor);
        }
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
    
    @Override
    public String toString()
    {
        if (name != null)
        {
            return "InterceptingChainMessageProcessorBuilder '" + name + "'";
        }
        else
        {
            return super.toString();
        }
    }

    class InterceptingMessageProcessorAdapter extends AbstractInterceptingMessageProcessor implements Disposable
    {
        private MessageProcessor delegate;

        public InterceptingMessageProcessorAdapter(MessageProcessor mp)
        {
            this.delegate = mp;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Invoking adapted MessageProcessor '" + delegate.getClass().getName() + "'");
            }
            // If the next message processor is an outbound router then create
            // outbound event
            if (delegate instanceof OutboundEndpoint)
            {
                event = new DefaultMuleEvent(event.getMessage(), (OutboundEndpoint) delegate,
                    event.getSession());
            }
            MuleEvent delegateResult = delegate.process(event);
            if (delegateResult != null)
            {
                return processNext(delegateResult);
            }
            else if (event.getFlowConstruct() instanceof SimpleFlowConstruct)
            {
                return processNext(OptimizedRequestContext.criticalSetEvent(event));
            }
            else
            {
                return null;
            }
        }

        public void setNext(MessageProcessor next)
        {
            this.next = next;
        }

        public void dispose()
        {
            this.delegate = null;
        }

        @Override
        public String toString()
        {
            return "InterceptingMessageProcessorAdapter [ target = '" + delegate.getClass().getName() + "' ]";
        }
    }

}
