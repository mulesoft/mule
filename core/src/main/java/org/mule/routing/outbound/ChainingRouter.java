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

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;

/**
 * <code>ChainingRouter</code> is used to pass a Mule event through multiple
 * endpoints using the result of the first as the input for the second.
 */
public class ChainingRouter extends FilteringOutboundRouter
{

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("endpoints"), this);
        }
    }

    @Override
    public MuleMessage route(MuleMessage message, MuleSession session) throws RoutingException
    {
        MuleMessage resultToReturn = null;
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        final int endpointsCount = endpoints.size();
        if (logger.isDebugEnabled())
        {
            logger.debug("About to chain " + endpointsCount + " endpoints.");
        }

        // need that ref for an error message
        OutboundEndpoint endpoint = null;
        try
        {
            MuleMessage intermediaryResult = message;

            for (int i = 0; i < endpointsCount; i++)
            {
                endpoint = getEndpoint(i, intermediaryResult);
                // if it's not the last endpoint in the chain,
                // enforce the synchronous call, otherwise we lose response
                boolean lastEndpointInChain = (i == endpointsCount - 1);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Sending Chained message '" + i + "': "
                                 + (intermediaryResult == null ? "null" : intermediaryResult.toString()));
                }

                if (!lastEndpointInChain)
                {
                    MuleMessage localResult = send(session, intermediaryResult, endpoint);
                    // Need to propagate correlation info and replyTo, because there
                    // is no guarantee that an external system will preserve headers
                    // (in fact most will not)
                    if (localResult != null &&
                        // null result can be wrapped in a NullPayload
                        localResult.getPayload() != NullPayload.getInstance() &&
                        intermediaryResult != null)
                    {
                        processIntermediaryResult(localResult, intermediaryResult);
                    }
                    intermediaryResult = localResult;

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Received Chain result '" + i + "': "
                                     + (intermediaryResult != null ? intermediaryResult.toString() : "null"));
                    }

                    if (intermediaryResult == null || intermediaryResult.getPayload() == NullPayload.getInstance())
                    {
                        // if there was an error in the first link of the chain, make sure we propagate back
                        // any exception payloads alongside the NullPayload
                        resultToReturn = intermediaryResult;
                        logger.warn("Chaining router cannot process any further endpoints. "
                                    + "There was no result returned from endpoint invocation: " + endpoint);
                        break;
                    }
                }
                else
                {
                    // ok, the last call,
                    // use the 'sync/async' method parameter
                    if (endpoint.isSynchronous())
                    {
                        resultToReturn = send(session, intermediaryResult, endpoint);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Received final Chain result '" + i + "': "
                                         + (resultToReturn == null ? "null" : resultToReturn.toString()));
                        }
                    }
                    else
                    {
                        // reset the previous call result to avoid confusion
                        resultToReturn = null;
                        dispatch(session, intermediaryResult, endpoint);
                    }
                }
            }

        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
        }
        return resultToReturn;
    }

    /**
     * Process intermediary result of invocation. The method will be invoked
     * <strong>only</strong> if both local and intermediary results are available
     * (not null).
     * <p/>
     * Overriding methods must call <code>super(localResult, intermediaryResult)</code>,
     * unless they are modifying the correlation workflow (if you know what that means,
     * you know what you are doing and when to do it).
     * <p/>
     * Default implementation propagates
     * the following properties:
     * <ul>
     * <li>correlationId
     * <li>correlationSequence
     * <li>correlationGroupSize
     * <li>replyTo
     * </ul>
     * @param localResult result of the last endpoint invocation
     * @param intermediaryResult the message travelling across the endpoints
     */
    protected void processIntermediaryResult(MuleMessage localResult, MuleMessage intermediaryResult)
    {
        localResult.setCorrelationId(intermediaryResult.getCorrelationId());
        localResult.setCorrelationSequence(intermediaryResult.getCorrelationSequence());
        localResult.setCorrelationGroupSize(intermediaryResult.getCorrelationGroupSize());
        localResult.setReplyTo(intermediaryResult.getReplyTo());
    }
}
