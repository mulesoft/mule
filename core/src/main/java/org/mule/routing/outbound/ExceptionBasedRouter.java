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

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>ExceptionBasedRouter</code> Will send the current event to the first
 * endpoint that doesn't throw an exception. If all attempted endpoints fail then an
 * exception is thrown. <p/> The router will override the sync/async mode of the
 * endpoint and force the sync mode for all endpoints except the last one.
 * <code>remoteSync</code> is also enforced.
 */

public class ExceptionBasedRouter extends FilteringOutboundRouter
{

    public MuleMessage route(MuleMessage message, MuleSession session, boolean synchronous)
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

        MuleMessage result = null;
        // need that ref for an error message
        OutboundEndpoint endpoint = null;
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
                    catch (MuleException e)
                    {
                        if(logger.isWarnEnabled())
                        {
                            Throwable t = ExceptionHelper.getRootException(e);
                            logger.warn("Failed to send to endpoint: " + endpoint.getEndpointURI().toString()
                                    + ". Error was: " + t + ". Trying next endpoint", t);
                        }
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
                    catch (MuleException e)
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

//    public void addEndpoint(Endpoint endpoint)
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
    protected boolean exceptionPayloadAvailable(MuleMessage message)
    {
        if (message == null)
        {
            return false;
        }

        final ExceptionPayload exceptionPayload = message.getExceptionPayload();
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
