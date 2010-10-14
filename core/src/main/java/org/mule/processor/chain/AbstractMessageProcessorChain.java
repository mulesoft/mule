/*
 * $Id: InterceptingChainCompositeMessageProcessor.java 19207 2010-08-26 05:02:51Z dirk.olmes $
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
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.util.StringUtils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if
 * this chain is nested in another chain the next MessageProcessor in the parent chain is not injected into
 * the first in the nested chain.
 */
public abstract class AbstractMessageProcessorChain extends AbstractInterceptingMessageProcessor implements MessageProcessorChain
{
    protected final transient Log log = LogFactory.getLog(getClass());
    protected String name;
    protected List<MessageProcessor> processors;

    public AbstractMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        this.name = name;
        this.processors = processors;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        System.out.println(this);
        if (log.isDebugEnabled())
        {
            log.debug(String.format("Invoking %s with event %s", this, event));
        }
        if (event == null)
        {
            return null;
        }

        return processNext(doProcess(event));
    }

    protected abstract MuleEvent doProcess(MuleEvent event) throws MuleException;

    public void initialise() throws InitialisationException
    {
        for (MessageProcessor processor : processors)
        {
            // MULE-5002 TODO review MP Lifecycle
            if (processor instanceof Initialisable /* && !(processor instanceof Transformer) */)
            {
                ((Initialisable) processor).initialise();
            }
        }
    }

    public void start() throws MuleException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Startable)
            {
                ((Startable) processor).start();
            }
        }
    }

    public void stop() throws MuleException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Stoppable)
            {
                ((Stoppable) processor).stop();
            }
        }
    }

    public void dispose()
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
        processors.clear();
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
            if (processor instanceof MuleContextAware)
            {
                ((MuleContextAware) processor).setMuleContext(flowConstruct.getMuleContext());
            }
        }
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        StringBuffer string = new StringBuffer();
        string.append(getClass().getSimpleName());
        if (name != null)
        {
            string.append(String.format(" '%s' ", name));
        }

        Iterator<MessageProcessor> mpIterator = processors.iterator();

        final String nl = String.format("%n");

        if (mpIterator.hasNext())
        {
            string.append(String.format("%n[ "));
            while (mpIterator.hasNext())
            {
                MessageProcessor mp = mpIterator.next();
                final String indented = StringUtils.replace(mp.toString(), nl, String.format("%n  "));
                string.append(String.format("%n  %s", indented));
                if (mpIterator.hasNext())
                {
                    string.append(", ");
                }
            }
            string.append(String.format("%n]"));
        }

        return string.toString();
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return processors;
    }
}
