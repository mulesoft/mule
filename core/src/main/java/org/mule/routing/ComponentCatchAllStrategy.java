/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.impl.MuleEvent;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.ComponentRoutingException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ComponentCatchAllStrategy</code> is used to catch any events and
 * forward the events to the component as is.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentCatchAllStrategy extends AbstractCatchAllStrategy
{
    public void setEndpoint(UMOEndpoint endpoint)
    {
        throw new UnsupportedOperationException("The endpoint cannot be set on this catch all");
    }

    public UMOEndpoint getEndpoint()
    {
        return null;
    }

    public synchronized UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
            throws RoutingException
    {
        UMOEvent event = RequestContext.getEvent();
        try {
            event = new MuleEvent(message, event.getEndpoint(), session.getComponent(), event);
            if (synchronous) {
                statistics.incrementRoutedMessage(event.getEndpoint());
                logger.info("Event being routed from catch all strategy for endpoint: "
                        + RequestContext.getEvent().getEndpoint());
                return session.getComponent().sendEvent(event);
            } else {
                statistics.incrementRoutedMessage(event.getEndpoint());
                session.getComponent().dispatchEvent(event);
                return null;
            }
        } catch (UMOException e) {
            throw new ComponentRoutingException(event.getMessage(), event.getEndpoint(), session.getComponent(), e);
        }
    }
}
