/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.AbstractRouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SelectiveConsumer</code> is an inbound router used to filter out unwanted
 * events. The filtering is performed by a <code>Filter</code> that can be set
 * on the router.
 * 
 * @see InboundRouter
 * @see org.mule.api.routing.InboundRouterCollection
 * @see org.mule.api.routing.RouterCollection
 */

public class SelectiveConsumer extends AbstractRouter implements InboundRouter
{
    protected final Log logger = LogFactory.getLog(getClass());

    private volatile Filter filter;
    private volatile boolean transformFirst = true;

    public boolean isMatch(MuleEvent event) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Attempting to route event: " + event.getId());
        }

        if (filter == null)
        {
            return true;
        }

        MuleMessage message = event.getMessage();

        if (transformFirst)
        {
            try
            {
                Object payload = event.transformMessage();
                message = new DefaultMuleMessage(payload, message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(
                    CoreMessages.transformFailedBeforeFilter(), event.getMessage(), 
                    event.getEndpoint(), e);
            }
        }

        boolean result = filter.accept(message);

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleEvent " + event.getId() + (result ? " passed filter " : " did not pass filter ")
                            + filter.getClass().getName());
        }

        return result;
    }

    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        if (this.isMatch(event))
        {
            return new MuleEvent[]{event};
        }
        else
        {
            return null;
        }
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    public boolean isTransformFirst()
    {
        return transformFirst;
    }

    public void setTransformFirst(boolean transformFirst)
    {
        this.transformFirst = transformFirst;
    }
}
