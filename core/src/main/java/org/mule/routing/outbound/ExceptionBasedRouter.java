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

import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ExceptionBasedRouter</code> Will send the current event to the first
 * endpoint that doesn't throw an exception. If all attempted endpoints fail then an
 * exception is thrown. <p/> The router will override the sync/async mode of the
 * endpoint and force the sync mode for all endpoints except the last one.
 * <code>remoteSync</code> is also enforced.
 */

public class ExceptionBasedRouter extends FilteringOutboundRouter
{

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        final int endpointsCount = endpoints.size();

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
                message.setCorrelationGroupSize(endpointsCount);
            }
        }

        UMOMessage result = null;
        // need that ref for an error message
        UMOImmutableEndpoint endpoint = null;
        boolean success = false;

        synchronized (endpoints)
        {
            for (int i = 0; i < endpointsCount; i++)
            {
                // apply endpoint URI templates if any
                endpoint = getEndpoint(i, message);
                boolean lastEndpoint = (i == endpointsCount - 1);

                if (!lastEndpoint)
                {
                    logger.info("Sync mode will be forced for " + endpoint.getEndpointURI()
                                + ", as there are more endpoints available.");
                }

                if (!lastEndpoint || synchronous)
                {
                    try
                    {
                        result = send(session, message, endpoint);
                        if (!exceptionPayloadAvailable(result))
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Successful invocation detected, stopping further processing.");
                            }
                            success = true;
                            break;
                        }
                    }
                    catch (UMOException e)
                    {
                        logger.warn("Failed to send to endpoint: " + endpoint.getEndpointURI().toString()
                                    + ". Error was: " + ExceptionHelper.getRootException(e) + ". Trying next endpoint");
                    }
                }
                else
                {
                    try
                    {
                        dispatch(session, message, endpoint);
                        success = true;
                        break;
                    }
                    catch (UMOException e)
                    {
                        logger.info("Failed to dispatch to endpoint: " + endpoint.getEndpointURI().toString()
                                    + ". Error was: " + e.getMessage() + ". Trying next endpoint");
                    }
                }
            }
        }

        if (!success)
        {
            throw new CouldNotRouteOutboundMessageException(message, endpoint);
        }

        return result;
    }

//    public void addEndpoint(UMOEndpoint endpoint)
//    {
//        if (!endpoint.isRemoteSync())
//        {
//            logger.debug("Endpoint: "
//                         + endpoint.getEndpointURI()
//                         + " registered on ExceptionBasedRouter needs to be RemoteSync enabled. Setting this property now.");
//            endpoint.setRemoteSync(true);
//        }
//        super.addEndpoint(endpoint);
//    }

    /**
     * @param message message to check
     * @return true if there was an exception payload set
     */
    protected boolean exceptionPayloadAvailable(UMOMessage message)
    {
        if (message == null)
        {
            return false;
        }

        final UMOExceptionPayload exceptionPayload = message.getExceptionPayload();
        if (exceptionPayload != null)
        {
            logger.info("Failure returned, will try next endpoint. Exception payload is: " + exceptionPayload);
            return true;
        }
        else
        {
            return false;
        }
    }
}
