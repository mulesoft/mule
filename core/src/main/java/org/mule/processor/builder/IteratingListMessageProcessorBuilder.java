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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.processor.NullMessageProcessor;

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
public class IteratingListMessageProcessorBuilder implements MessageProcessorBuilder
{

    protected List processors = new ArrayList();
    protected String name;

    public MessageProcessor build() throws MuleException
    {
        if (processors.isEmpty())
        {
            return new NullMessageProcessor();
        }
        return new IteratingListCompositeMessageProcessor(processors, name);
    }

    public void setName(String name)
    {
        this.name = name;
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

    @Override
    public String toString()
    {
        if (name != null)
        {
            return "IteratingListMessageProcessorBuilder '" + name + "'";
        }
        else
        {
            return super.toString();
        }
    }

    /**
     * Builder needs to return a composite rather than the first MessageProcessor in
     * the chain. This is so that if this chain is nested in another chain the next
     * MessageProcessor in the parent chain is not injected into the first in the
     * nested chain.
     */
    static class IteratingListCompositeMessageProcessor
        implements MessageProcessor, Lifecycle, FlowConstructAware, MuleContextAware
    {
        private Log log;
        private String name;
        private List<MessageProcessor> list = new ArrayList<MessageProcessor>();

        public IteratingListCompositeMessageProcessor(List processors, String name) throws MuleException
        {
            this.name = name;
            for (Object object : list)
            {
                list.add(getMessageProcessor(object));
            }
            log = LogFactory.getLog(IteratingListCompositeMessageProcessor.class);
        }

        private MessageProcessor getMessageProcessor(Object processor) throws MuleException
        {
            if (processor instanceof MessageProcessor)
            {
                return (MessageProcessor) processor;
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                return ((MessageProcessorBuilder) processor).build();
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (log.isDebugEnabled())
            {
                log.debug("Invoking " + this + " with event " + event);
            }
            MuleEvent result = event;
            for (MessageProcessor processor : list)
            {
                result = processor.process(result);
            }
            return result;
        }

        public void initialise() throws InitialisationException
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof Initialisable)
                {
                    ((Initialisable) processor).initialise();
                }
            }
        }

        public void start() throws MuleException
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof Startable)
                {
                    ((Startable) processor).start();
                }
            }
        }

        public void stop() throws MuleException
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof Stoppable)
                {
                    ((Stoppable) processor).stop();
                }
            }
        }

        public void dispose()
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof Disposable)
                {
                    ((Disposable) processor).dispose();
                }
            }
        }

        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof FlowConstructAware)
                {
                    ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
                }
            }
        }

        public void setMuleContext(MuleContext muleContext)
        {
            for (MessageProcessor processor : list)
            {
                if (processor instanceof MuleContextAware)
                {
                    ((MuleContextAware) processor).setMuleContext(muleContext);
                }
            }
        }

        @Override
        public String toString()
        {
            StringBuffer string = new StringBuffer();
            string.append("IteratingListCompositeMessageProcessor ");
            if (name != null)
            {
                string.append(" '" + name + "' ");
            }
            else
            {
                string.append("[ processors= ");
                Iterator<MessageProcessor> mpIterator = list.iterator();
                while (mpIterator.hasNext())
                {
                    MessageProcessor mp = mpIterator.next();
                    string.append(mp.getClass().getName());
                    if (mpIterator.hasNext())
                    {
                        string.append(", ");
                    }
                }
                string.append(" ]");
            }
            return string.toString();
        }
    }

}
