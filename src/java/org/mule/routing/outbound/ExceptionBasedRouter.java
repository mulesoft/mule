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
package org.mule.routing.outbound;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ExceptionBasedRouter</code> Will send the current event to the first endpoint
 * that doesn't throw an exception. If all attempted endpoints fail then an exception is
 * thrown.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ExceptionBasedRouter extends FilteringOutboundRouter
{

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        UMOMessage result = null;
        if (endpoints == null || endpoints.size() == 0) {
            throw new RoutePathNotFoundException(new Message(Messages.NO_ENDPOINTS_FOR_ROUTER), message, null);
        }
        if (enableCorrelation != ENABLE_CORRELATION_NEVER) {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET)) {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            } else {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(endpoints.size());
            }
        }

            UMOEndpoint endpoint;
        boolean success = false;
        synchronized (endpoints) {
            for (int i = 0; i < endpoints.size(); i++) {
                endpoint = (UMOEndpoint) endpoints.get(i);
                if (synchronous) {
                    // Were we have multiple outbound endpoints
                    if (result == null) {
                        try {
                            result = send(session, message, endpoint);
                            success = true;
                            break;
                        } catch (UMOException e) {
                            logger.info("Failed to send to endpoint: " + endpoint.getEndpointURI().toString() +
                                    ". Error was: " + e.getMessage() + ". Trying next endpoint");
                        }
                    } else {
                        String def = (String) endpoint.getProperties().get("default");
                        if (def != null) {
                            try {
                                result = send(session, message, endpoint);
                                success = true;
                                break;
                            } catch (UMOException e) {
                                logger.info("Failed to send to endpoint: " + endpoint.getEndpointURI().toString() +
                                        ". Error was: " + e.getMessage() + ". Trying next endpoint");
                            }
                        } else {
                            try {
                                send(session, message, endpoint);
                                success = true;
                                break;
                            } catch (UMOException e) {
                                logger.info("Failed to send to endpoint: " + endpoint.getEndpointURI().toString() +
                                        ". Error was: " + e.getMessage() + ". Trying next endpoint");
                            }
                        }
                    }
                } else {
                    try {
                        dispatch(session, message, endpoint);
                        success = true;
                        break;
                    } catch (UMOException e) {
                        logger.info("Failed to disparch to endpoint: " + endpoint.getEndpointURI().toString() +
                                ". Error was: " + e.getMessage() + ". Trying next endpoint");
                    }
                }
            }
        }
        if(!success) {
            throw new CouldNotRouteOutboundMessageException(message, (UMOEndpoint) endpoints.get(0));
        }
        return result;
    }
}
