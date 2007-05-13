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

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>MulticastingRouter</code> will broadcast the current message to every
 * endpoint registed with the router.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MulticastingRouter extends FilteringOutboundRouter
{

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        UMOMessage result = null;
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }
        if (enableCorrelation != ENABLE_CORRELATION_NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
            {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            }
            else
            {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(endpoints.size());
            }
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
                        // Were we have multiple outbound endpoints
                        if (result == null)
                        {
                            result = send(session, message, endpoint);
                        }
                        else
                        {
                            String def = (String) endpoint.getProperties().get("default");
                            if (def != null)
                            {
                                result = send(session, message, endpoint);
                            }
                            else
                            {
                                send(session, message, endpoint);
                            }
                        }
                    }
                    else
                    {
                        dispatch(session, message, endpoint);
                    }
                }
            }
        }
        catch (UMOException e)
        {
            throw new CouldNotRouteOutboundMessageException(message, (UMOEndpoint) endpoints.get(0), e);
        }
        return result;
    }
}
