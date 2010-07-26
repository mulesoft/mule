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

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.filter.Filter;
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
 * @deprecated
 */
public class SelectiveConsumer extends AbstractRouter implements InboundRouter
{
    protected final Log logger = LogFactory.getLog(getClass());

    private volatile Filter filter;

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

        boolean result = filter.accept(event.getMessage());

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleEvent " + event.getId() + (result ? " passed filter " : " did not pass filter ")
                            + filter.getClass().getName());
        }

        return result;
    }

    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        return new MuleEvent[]{event};
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }
}
