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
package org.mule.routing.outbound;

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>MulticastingRouter</code> will broadcast the current message to every endpoint registed
 * with the router.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MulticastingRouter extends FilteringOutboundRouter
{

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        UMOMessage result = null;
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutingException("No endpoints are set on this router, cannot route message");
        }
        try
        {
            message.setCorrelationId(message.getUniqueId());
            message.setCorrelationGroupSize(endpoints.size());
        } catch (UniqueIdNotSupportedException e)
        {
            throw new RoutingException("Cannot use multicasting router with transports that do not support a unique id", e, message);
        }

        try
        {
            UMOEndpoint endpoint;
            synchronized (endpoints)
            {
                for (int i = 0; i < endpoints.size(); i++)
                {
                    endpoint = (UMOEndpoint) endpoints.get(i);
                    if (synchronous)
                    {
                        result = send(session, message, (UMOEndpoint) endpoint);
                    } else
                    {
                        dispatch(session, message, (UMOEndpoint) endpoint);
                    }
                }
            }
        } catch (UMOException e)
        {
            throw new CouldNotRouteOutboundMessageException(e.getMessage(), e, message);
        }
        return result;
    }
}
