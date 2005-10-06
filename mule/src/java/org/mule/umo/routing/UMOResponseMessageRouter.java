/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.UMOTransformer;

import java.util.List;

/**
 * <code>UMOResponseMessageRouter</code> is a router that can be used to
 * control how the response in a request/response message flow is created. Main
 * usecase is to aggregate a set of asynchonous events into a single response
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOResponseMessageRouter extends UMORouterCollection
{
    void route(UMOEvent event) throws RoutingException;

    UMOMessage getResponse(UMOMessage message) throws UMOException;

    UMOResponseRouter removeRouter(UMOResponseRouter router);

    void addEndpoint(UMOEndpoint endpoint);

    boolean removeEndpoint(UMOEndpoint endpoint);

    List getEndpoints();

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see UMOResponseMessageRouter
     */
    UMOEndpoint getEndpoint(String name);

    void setEndpoints(List endpoints);

    public int getTimeout();

    public void setTimeout(int timeout);
}
