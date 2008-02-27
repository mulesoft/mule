/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;

import java.util.List;

/**
 * <code>DefaultInboundRouterCollection</code> manages a collection of inbound routers.
 */

public interface InboundRouterCollection extends RouterCollection
{
    MuleMessage route(MuleEvent event) throws MessagingException;

    void addRouter(InboundRouter router);

    InboundRouter removeRouter(InboundRouter router);

    void addEndpoint(InboundEndpoint endpoint);

    boolean removeEndpoint(InboundEndpoint endpoint);

    List getEndpoints();

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see InboundRouterCollection
     */
    InboundEndpoint getEndpoint(String name);

    void setEndpoints(List endpoints);
}
