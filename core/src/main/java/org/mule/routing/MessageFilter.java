/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
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
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractFilteringMessageProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link InterceptingMessageProcessor} that filters message flow
 * using a {@link Filter}. Is the filter accepts the message then message flow
 * continues to the next message processor. If the filter does not accept the message
 * processor and a message processor is configured for handling unaccepted message
 * then this will be invoked, otherwise <code>null</code> will be returned.
 * <p/>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Filter.html">http://www.eaipatterns
 * .com/Filter.html<a/>
 */
public class MessageFilter extends AbstractFilteringMessageProcessor implements FlowConstructAware, Lifecycle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFilter.class);

    protected Filter filter;

    /**
     * For IoC only
     * 
     * @deprecated Use MessageFilter(Filter filter)
     */
    @Deprecated
    public MessageFilter()
    {
        super();
    }

    public MessageFilter(Filter filter)
    {
        super();
        this.filter = filter;
    }

    /**
     * @param filter
     * @param throwExceptionOnUnaccepted throw a FilterUnacceptedException when a
     *            message is rejected by the filter?
     * @param messageProcessor used to handler unaccepted messages
     */
    public MessageFilter(Filter filter, boolean throwExceptionOnUnaccepted, MessageProcessor messageProcessor)
    {
        this.filter = filter;
        this.throwOnUnaccepted = throwExceptionOnUnaccepted;
        this.unacceptedMessageProcessor = messageProcessor;
        setUnacceptedMessageProcessor(unacceptedMessageProcessor);
    }

    @Override
    protected boolean accept(MuleEvent event)
    {
        if (filter == null)
        {
            return true;
        }

        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        {
            return filter.accept(event.getMessage());
        }
        else
        {
            return false;
        }
    }

    @Override
    protected MuleException filterUnacceptedException(MuleEvent event)
    {
        return new FilterUnacceptedException(CoreMessages.messageRejectedByFilter(), event, filter, this);
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    @Override
    public String toString()
    {
        return (filter == null ? "null filter" : filter.getClass().getName()) + " (wrapped by "
               + this.getClass().getSimpleName() + ")";
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) unacceptedMessageProcessor).setMuleContext(context);
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) unacceptedMessageProcessor).setFlowConstruct(flowConstruct);
        }
    }

    public void initialise() throws InitialisationException
    {
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Initialisable)
        {
            ((Initialisable) unacceptedMessageProcessor).initialise();
        }

        LifecycleUtils.initialiseIfNeeded(filter);
    }

    public void start() throws MuleException
    {
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Startable)
        {
            ((Startable) unacceptedMessageProcessor).start();
        }
    }

    public void stop() throws MuleException
    {
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Stoppable)
        {
            ((Stoppable) unacceptedMessageProcessor).stop();
        }
    }

    public void dispose()
    {
        if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Disposable)
        {
            ((Disposable) unacceptedMessageProcessor).dispose();
        }

        LifecycleUtils.disposeIfNeeded(filter, LOGGER);
    }
}
