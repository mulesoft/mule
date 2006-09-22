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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * <code>ChainingRouter</code> is used to pass a Mule event through multiple
 * endpoints using the result of the first and the input for the second
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ChainingRouter extends FilteringOutboundRouter
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ChainingRouter.class);

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        UMOMessage resultToReturn = null;
        final int endpointsCount = endpoints.size();
        if (endpoints == null || endpointsCount == 0) {
            throw new RoutePathNotFoundException(new Message(Messages.NO_ENDPOINTS_FOR_ROUTER), message, null);
        }

        logger.debug("About to chain " + endpointsCount + " endpoints.");

        // need that ref for an error message
        UMOEndpoint endpoint = null;
        try {
            UMOMessage intermediaryResult = message;

            for (int i = 0; i < endpointsCount; i++) {
                endpoint = getEndpoint(i, intermediaryResult);
                // if it's not the last endpoint in the chain,
                // enforce the synchronous call, otherwise we lose response
                boolean lastEndpointInChain = (i == endpointsCount - 1);
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending Chained message '" + i + "': " + (intermediaryResult==null ? "null" : intermediaryResult.toString()));
                }
                if (!lastEndpointInChain) {

                    UMOMessage tempResult = send(session, intermediaryResult, endpoint);
                    // Need to propagate correlation info and replyTo, because there
                    // is no guarantee that an external system will preserve headers
                    // (in fact most will not)
                    if (tempResult != null && intermediaryResult != null) {
                        tempResult.setCorrelationId(intermediaryResult.getCorrelationId());
                        tempResult.setCorrelationSequence(intermediaryResult.getCorrelationSequence());
                        tempResult.setCorrelationGroupSize(intermediaryResult.getCorrelationGroupSize());
                        tempResult.setReplyTo(intermediaryResult.getReplyTo());
                    }
                    intermediaryResult = tempResult;

                    if (logger.isDebugEnabled()) {
                        logger.debug("Received Chain result '" + i + "': "
                            + (intermediaryResult != null ? intermediaryResult.toString() : "null"));
                    }
                    if (intermediaryResult == null) {
                        logger.warn("Chaining router cannot process any further endpoints. "
                                + "There was no result returned from endpoint invocation: " + endpoint);
                        break;
                    }
                } else {
                    // ok, the last call,
                    // use the 'sync/async' method parameter
                    if (synchronous) {
                        resultToReturn = send(session, intermediaryResult, endpoint);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received final Chain result '" + i + "': "
                                + (resultToReturn == null ? "null" : resultToReturn.toString()));
                        }
                    } else {
                        // reset the previous call result to avoid confusion
                        resultToReturn = null;
                        dispatch(session, intermediaryResult, endpoint);
                    }
                }
            }

        } catch (UMOException e) {
            throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
        }
        return resultToReturn;
    }

    public void addEndpoint(UMOEndpoint endpoint) {
        if(!endpoint.isRemoteSync()) {
            logger.debug("Endpoint: " + endpoint.getEndpointURI() + " registered on chaining router needs to be RemoteSync enabled. Setting this property now");
            endpoint.setRemoteSync(true);
        }
        super.addEndpoint(endpoint);
    }


}
