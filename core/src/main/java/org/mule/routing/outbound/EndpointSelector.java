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

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import java.util.Iterator;

/**
 * <code>EndpointSelector</code> selects the outgoing endpoint based on a
 * message property ("endpoint" by default).  It will first try to match the
 * endpoint by name and then by address.
 * <pre>
 *
 * &lt;outbound-router&gt;
 *      &lt;router className="org.mule.routing.outbound.EndpointSelector"&gt;
 *          &lt;endpoint name="dest1" address="jms://queue1" /&gt;
 *          &lt;endpoint name="dest2" address="jms://queue2" /&gt;
 *          &lt;endpoint name="dest3" address="jms://queue3" /&gt;
 *          &lt;properties&gt;
 *              &lt;property name="selector" value="endpoint" /&gt;
 *          &lt;/properties&gt;
 *      &lt;/router&gt;
 * &lt;/outbound-router&gt;
 *
 * </pre>
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
            ep = (UMOEndpoint) iterator.next();
            // Endpoint identifier (deprecated)
            if (endpointName.equals(ep.getEndpointURI().getEndpointName()))
            {
                return ep;
            }
            // Global endpoint
            else if (endpointName.equals(ep.getName()))
            {
                return ep;
            }
            else if (endpointName.equals(ep.getEndpointURI().getUri().toString()))
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
