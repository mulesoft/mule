/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.List;

/**
 * <code>InboundRouterCollection</code> manages a collection of inbound routers
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOInboundMessageRouter  extends UMORouterCollection
{
    public UMOMessage route(UMOEvent event) throws RoutingException;

    public void addRouter(UMOInboundRouter router);

    public UMOInboundRouter removeRouter(UMOInboundRouter router);

    public void addEndpoint(UMOEndpoint endpoint);

    public boolean removeEndpoint(UMOEndpoint endpoint);

    public List getEndpoints();

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see UMOInboundMessageRouter
     */
    public UMOEndpoint getEndpoint(String name);

    public void setEndpoints(List endpoints);
}
