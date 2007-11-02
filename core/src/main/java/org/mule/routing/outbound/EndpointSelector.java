/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.StringUtils;
import org.mule.util.properties.PropertyExtractorManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class EndpointSelector extends FilteringOutboundRouter implements ManagementContextAware
{
    public static final String DEFAULT_SELECTOR_PROPERTY = "endpoint";

    private String selectorProperty = DEFAULT_SELECTOR_PROPERTY;



    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        List endpoints;
        String endpointName;
        
        Object property = PropertyExtractorManager.processExpression(getSelectorProperty(), message);
        if(property ==null)
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.propertyIsNotSetOnEvent(getSelectorProperty()), message, null);
        }

        if (property instanceof String)
        {
            endpoints = new ArrayList(1);
            endpoints.add(property);
        }
        else if(property instanceof List)
        {
            endpoints = (List)property;
        }
        else
        {
            throw new CouldNotRouteOutboundMessageException(CoreMessages.propertyIsNotSupportedType(
                    getSelectorProperty(), new Class[]{String.class, List.class}, property.getClass()), message, null);

        }

        UMOMessage result = null;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointName =  iterator.next().toString();

            if(StringUtils.isEmpty(endpointName))
            {
                throw new CouldNotRouteOutboundMessageException(
                        CoreMessages.objectIsNull("Endpoint Name: " + getSelectorProperty()), message, null);
            }
            UMOImmutableEndpoint ep = null;
            try
            {
                ep = lookupEndpoint(endpointName);
                if (ep == null)
                {
                    throw new CouldNotRouteOutboundMessageException(CoreMessages.objectNotFound("Endpoint",
                        endpointName), message, ep);
                }
                if (synchronous)
                {
                    // TODO See MULE-2613, we only return the last message here
                    result = send(session, message, ep);
                }
                else
                {
                    dispatch(session, message, ep);
                }
            }
            catch (UMOException e)
            {
                throw new CouldNotRouteOutboundMessageException(message, ep, e);
            }
        }
        return result;
    }

    protected UMOImmutableEndpoint lookupEndpoint(String endpointName) throws UMOException
    {
        UMOImmutableEndpoint ep;
        Iterator iterator = endpoints.iterator();
        while (iterator.hasNext())
        {
            ep = (UMOImmutableEndpoint) iterator.next();
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
        return getManagementContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName,
            getManagementContext());
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
