/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;

/**
 * A MessageProcessor that tees an event off to another processor
 */
public class WireTapMessageProcessor extends AbstractMessageObserver

{
    private volatile MessageProcessor tap;
    private volatile Filter filter;
    private FilteringMessageProcessor filterProcessor;

    @Override
    public void observe(MuleEvent event)
    {
        if (tap == null)
        {
            return;
        }

        try
        {
            if (filterProcessor != null)
            {
                event = filterProcessor.process(event);
                if (event == null)
                {
                    return;
                }
            }

            RequestContext.setEvent(null);
            tap.process(RequestContext.clomeEvent(event, tap));
        }
        catch (MuleException e)
        {
            logger.debug("Exception sending to wiretap", e);
        }
    }

    public MessageProcessor getTap()
    {
        return tap;
    }

    public void setTap(MessageProcessor tap)
    {
        this.tap = tap;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
        if (filter != null)
        {
            FilteringMessageProcessor processor = new FilteringMessageProcessor();
            processor.setFilter(filter);
            filterProcessor = processor;
        }
        else
        {
            filterProcessor = null;
        }
    }
}