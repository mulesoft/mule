/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.routing.AbstractRouter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.umo.routing.UMORouterCollection;
import org.mule.umo.transformer.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SelectiveConsumer</code> is an inbound router used to filter out unwanted
 * events. The filtering is performed by a <code>UMOFilter</code> that can be set
 * on the router.
 * 
 * @see UMOInboundRouter
 * @see UMOInboundRouterCollection
 * @see UMORouterCollection
 */

public class SelectiveConsumer extends AbstractRouter implements UMOInboundRouter
{
    protected final Log logger = LogFactory.getLog(getClass());

    private volatile UMOFilter filter;
    private volatile boolean transformFirst = true;

    public boolean isMatch(UMOEvent event) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Attempting to route event: " + event.getId());
        }

        if (filter == null)
        {
            return true;
        }

        UMOMessage message = event.getMessage();

        if (transformFirst)
        {
            try
            {
                Object payload = event.getTransformedMessage();
                message = new MuleMessage(payload, message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(
                    new Message(Messages.TRANSFORM_FAILED_BEFORE_FILTER), 
                    event.getMessage(), event.getEndpoint(), e);
            }
        }

        boolean result = filter.accept(message);

        if (logger.isDebugEnabled())
        {
            logger.debug("Event " + event.getId() + (result ? " passed filter " : " did not pass filter ")
                            + filter.getClass().getName());
        }

        return result;
    }

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (this.isMatch(event))
        {
            return new UMOEvent[]{event};
        }
        else
        {
            return null;
        }
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
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
