/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import java.util.Iterator;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>EndpointSelector</code> selects the outgoing endpoint by name based on a
 * message property ("endpoint" by default). <outbound-router> <router
 * className="org.mule.routing.outbound.EndpointSelector"> <endpoint name="dest1"
 * address="jms://queue1" /> <endpoint name="dest2" address="jms://queue2" />
 * <endpoint name="dest3" address="jms://queue3" /> <properties> <property
 * name="selector" value="endpoint" /> </properties> </router> </outbound-router>
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class EndpointSelector extends FilteringOutboundRouter
{

    private String selectorProperty = "endpoint";

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        String endpointName = message.getStringProperty(getSelectorProperty(), null);
        if (endpointName == null)
        {
            throw new IllegalArgumentException("selectorProperty '" + getSelectorProperty()
                                               + "' must be set on message in order to route it.");
        }

        UMOEndpoint ep = lookupEndpoint(endpointName);
        if (ep == null)
        {
            throw new CouldNotRouteOutboundMessageException(
                Message.createStaticMessage("No endpoint found with the name " + endpointName), message, ep);
        }

        try
        {
            if (synchronous)
            {
                return send(session, message, ep);
            }
            else
            {
                dispatch(session, message, ep);
                return null;
            }
        }
        catch (UMOException e)
        {
            throw new CouldNotRouteOutboundMessageException(message, ep, e);
        }
    }

    protected UMOEndpoint lookupEndpoint(String endpointName)
    {
        UMOEndpoint ep;
        Iterator iterator = endpoints.iterator();
        while (iterator.hasNext())
        {
            ep = (UMOEndpoint)iterator.next();
            if (endpointName.equals(ep.getName()))
            {
                return ep;
            }
        }
        return null;
    }

    public String getSelectorProperty()
    {
        return selectorProperty;
    }

    public void setSelectorProperty(String selectorProperty)
    {
        this.selectorProperty = selectorProperty;
    }
}
