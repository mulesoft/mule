/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.builder;

import org.mule.api.FlowConstruct;
import org.mule.api.FlowConstructAware;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class ChainMessageProcessorBuilder implements MessageProcessorBuilder
{

    protected List<MessageProcessor> chain = new ArrayList<MessageProcessor>();
    protected String name;

    public MessageProcessor build()
    {
        if (chain.isEmpty())
        {
            return new NullMessageProcessor();
        }

        InterceptingMessageProcessor first = createInterceptingMessageProcessor(chain.get(0));
        MessageProcessor composite = new ChainedCompositeMessageProcessor(first, chain, name);
        InterceptingMessageProcessor current = first;

        for (int i = 1; i < chain.size(); i++)
        {
            InterceptingMessageProcessor mp = createInterceptingMessageProcessor(chain.get(i));
            current.setListener(mp);
            current = mp;
        }
        return composite;
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

    public ChainMessageProcessorBuilder chain(MessageProcessor... processors)
    {
        for (MessageProcessor messageProcessor : processors)
        {
            chain.add(messageProcessor);
        }
        return this;
    }

    public ChainMessageProcessorBuilder chain(Collection <MessageProcessor> processors)
    {
        if (processors != null)
        {
            chain.addAll(processors);
        }
        return this;
    }

    public ChainMessageProcessorBuilder chainBefore(MessageProcessor processor)
    {
        chain.add(0, processor);
        return this;
    }
    
    // TODO BL-23 Temporary until inbound chain customization is implemented
    @Deprecated
    public void replaceMessageProcessor(Class processorClass, MessageProcessor replacement)
    {
        for (int i = 1; i < chain.size(); i++)
        {
            if (chain.get(i).getClass().equals(processorClass))
            {
                chain.set(i, replacement);
            }
        }
    }

    static class InterceptingMessageProcessorAdapter extends AbstractInterceptingMessageProcessor
    {
        private MessageProcessor delegate;

        public InterceptingMessageProcessorAdapter(MessageProcessor mp)
        {
            this.delegate = mp;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processNext(delegate.process(event));
        }

        public void setNext(MessageProcessor next)
        {
            this.next = next;
        }

        @Override
        public String toString()
        {
            return "InterceptingMessageProcessorAdapter[" + delegate.getClass().getName() + "]";
        }
    }

    /**
     * Builder needs to return a composite rather than the first MessageProcessor in
     * the chain. This is so that if this chain is nested in another chain the next
     * MessageProcessor in the parent chain is not injected into the first in the
     * nested chain.
     */
    static class ChainedCompositeMessageProcessor implements MessageProcessor, Lifecycle, FlowConstructAware
    {
        private Log log;
        private String name;
        private MessageProcessor firstInChain;
        private List<MessageProcessor> allProcessors;

        public ChainedCompositeMessageProcessor(InterceptingMessageProcessor firstInChain,
                                                List<MessageProcessor> allProcessors,
                                                String name)
        {
            this.name = name;
            this.firstInChain = firstInChain;
            this.allProcessors = allProcessors;
            log = LogFactory.getLog(ChainedCompositeMessageProcessor.class);
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            log.debug("Invoking " + this + " with event " + event);
            return firstInChain.process(event);
        }

        public void initialise() throws InitialisationException
        {
            for (MessageProcessor processor : allProcessors)
            {
                if (processor instanceof Initialisable)
                {
                    ((Initialisable) processor).initialise();
                }
            }
        }

        public void start() throws MuleException
        {
            for (MessageProcessor processor : allProcessors)
            {
                if (processor instanceof Startable)
                {
                    ((Startable) processor).start();
                }
            }
        }

        public void stop() throws MuleException
        {
            for (MessageProcessor processor : allProcessors)
            {
                if (processor instanceof Stoppable)
                {
                    ((Stoppable) processor).stop();
                }
            }
        }

        public void dispose()
        {
            for (MessageProcessor processor : allProcessors)
            {
                if (processor instanceof Disposable)
                {
                    ((Disposable) processor).dispose();
                }
            }
        }

        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            for (MessageProcessor processor : allProcessors)
            {
                if (processor instanceof FlowConstructAware)
                {
                    ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
                }
            }
        }
    }

    private static class NullMessageProcessor implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return event;
        }
    }
    
    @Override
    public String toString()
    {
        StringBuffer string = new StringBuffer();
        string.append("ChainedCompositeMessageProcessor [");
        if (name != null)
        {
            string.append("name=\"");
            string.append(name);
            string.append("\", ");
        }
        string.append("processors=");
        Iterator<MessageProcessor> mpIterator = chain.iterator();
        while (mpIterator.hasNext())
        {
            MessageProcessor mp = mpIterator.next();
            if (mp instanceof ChainedCompositeMessageProcessor)
            {
                string.append("ChainedCompositeMessageProcessor [name=\"");
                string.append(((ChainedCompositeMessageProcessor) mp).name);
                string.append("\"]");
            }
            else
            {
                string.append(mp.getClass().getName());
            }
            if (mpIterator.hasNext())
            {
                string.append(",");
            }
        }
        string.append("]");
        return string.toString();
    }

}
