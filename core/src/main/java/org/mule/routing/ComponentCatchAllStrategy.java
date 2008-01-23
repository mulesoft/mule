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

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.ComponentRoutingException;
import org.mule.api.routing.RoutingException;

/**
 * <code>ComponentCatchAllStrategy</code> is used to catch any events and forward the
 * events to the component as is.
 */
public class ComponentCatchAllStrategy extends AbstractCatchAllStrategy
{
    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        throw new UnsupportedOperationException("The endpoint cannot be set on this catch all");
    }

    public ImmutableEndpoint getEndpoint()
    {
        return null;
    }

    public synchronized MuleMessage catchMessage(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException
    {
        MuleEvent event = RequestContext.getEvent();
        logger.debug("Catch all strategy handling event: " + event);
        try
        {
            logger.info("MuleEvent being routed from catch all strategy for endpoint: " + event.getEndpoint());
            event = new DefaultMuleEvent(message, event.getEndpoint(), session.getComponent(), event);
            if (synchronous)
            {
                statistics.incrementRoutedMessage(event.getEndpoint());
                return session.getComponent().sendEvent(event);
            }
            else
            {
                statistics.incrementRoutedMessage(event.getEndpoint());
                session.getComponent().dispatchEvent(event);
                return null;
            }
        }
        catch (MuleException e)
        {
            throw new ComponentRoutingException(event.getMessage(), event.getEndpoint(),
                session.getComponent(), e);
        }
    }
}
