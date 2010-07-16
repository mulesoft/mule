/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.processor.AbstractMessageObserver;

/**
 * The <code>WireTap</code> MessageProcessor allows inspection of messages in a flow.
 * <p>
 * The incoming message is is sent to both the primary and wiretap outputs. The flow
 * of the primary output will be unmodified and a copy of the message used for the
 * wiretap output.
 * <p>
 * An optional filter can be used to filter which message are sent to the wiretap
 * output, this filter does not affect the flow to the primary output. If there is an
 * error sending to the wiretap output no exception will be thrown but rather an
 * error logged.
 * <p>
 * <b>EIP Reference:</b> {@link http://www.eaipatterns.com/WireTap.html}
 */
public class WireTap extends AbstractMessageObserver

{
    protected volatile MessageProcessor tap;
    protected volatile Filter filter;

    protected MessageProcessor filteredTap = new WireTapFilter();

    @Override
    public void observe(MuleEvent event)
    {
        if (tap == null)
        {
            return;
        }

        try
        {
            // Do we need this?
            RequestContext.setEvent(null);
            filteredTap.process(RequestContext.cloneAndUpdateEventEndpoint(event, tap));
        }
        catch (MuleException e)
        {
            logger.error("Exception sending to wiretap output " + tap, e);
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
    }

    private class WireTapFilter extends AbstractFilteringMessageProcessor
    {
        @Override
        protected boolean accept(MuleEvent event)
        {
            if (filter == null)
            {
                return true;
            }
            else
            {
                return filter.accept(event.getMessage());
            }
        }

        @Override
        protected MuleEvent processNext(MuleEvent event) throws MuleException
        {
            if (tap != null)
            {
                tap.process(event);
            }
            return null;
        }
    }

}
