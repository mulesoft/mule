/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractFilteringMessageProcessor;

/**
 * Implementation of {@link InterceptingMessageProcessor} that filters message flow using a {@link Filter}. Is
 * the filter accepts the message then message flow continues to the next message processor. If the filter
 * does not accept the message processor and a message processor is configured for handling unaccepted message
 * then this will be invoked, otherwise <code>null</code> will be returned.
 * <p/>
 * <b>EIP Reference:</b> {@link http://www.eaipatterns.com/Filter.html}
 */
public class MessageFilter extends AbstractFilteringMessageProcessor
{
    protected Filter filter;

    /** 
     * For IoC only
     * @deprecated Use MessageFilter(Filter filter) 
     */
    public MessageFilter()
    {
        // empty
    }

    public MessageFilter(Filter filter)
    {
        this.filter = filter;
    }

    /**
     * @param filter
     * @param throwExceptionOnUnaccepted throw a FilterUnacceptedException when a message is rejected by the filter?
     * @param messageProcessor used to handler unaccepted messages
     */
    public MessageFilter(Filter filter, boolean throwExceptionOnUnaccepted, MessageProcessor messageProcessor)
    {
        this.filter = filter;
        this.throwOnUnaccepted = throwExceptionOnUnaccepted;
        this.unacceptedMessageProcessor = messageProcessor;
    }

    @Override
    protected boolean accept(MuleEvent event)
    {
        if (filter == null)
        {
            return true;
        }

        if (event != null)
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
        return new FilterUnacceptedException(CoreMessages.messageRejectedByFilter(), event, filter);        
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
        return (filter == null ? "null filter" : filter.getClass().getName()) + " (wrapped by " + this.getClass().getSimpleName() + ")";
    }
}
