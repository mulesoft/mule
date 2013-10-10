/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;

/**
 * <code>ChainingRouter</code> is used to pass a Mule event through multiple
 * targets using the result of the first as the input for the second.
 */
public class ChainingRouter extends FilteringOutboundRouter
{
    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (routes == null || routes.size() == 0)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("targets"), this);
        }
    }

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleEvent resultToReturn = event;
        if (routes == null || routes.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        final int endpointsCount = routes.size();
        if (logger.isDebugEnabled())
        {
            logger.debug("About to chain " + endpointsCount + " targets.");
        }

        // need that ref for an error message
        MessageProcessor endpoint = null;
        try
        {
            MuleMessage intermediaryResult = event.getMessage();

            for (int i = 0; i < endpointsCount; i++)
            {
                endpoint = getRoute(i, resultToReturn);
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
                    MuleEvent endpointResult = sendRequest(resultToReturn, intermediaryResult, endpoint, true);
                    resultToReturn = endpointResult != null
                                     && VoidMuleEvent.getInstance().equals(endpointResult)
                                                                                          ? endpointResult
                                                                                          : resultToReturn;
                    MuleMessage localResult = endpointResult == null
                                              || VoidMuleEvent.getInstance().equals(endpointResult)
                                                                                                   ? null
                                                                                                   : endpointResult.getMessage();
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
                        resultToReturn = intermediaryResult == null ? null : new DefaultMuleEvent(intermediaryResult, resultToReturn);
                        logger.warn("Chaining router cannot process any further targets. "
                                    + "There was no result returned from endpoint invocation: " + endpoint);
                        break;
                    }
                }
                else
                {
                    // ok, the last call,
                    // use the 'sync/async' method parameter
                    resultToReturn = sendRequest(resultToReturn, intermediaryResult, endpoint, true);
                    if (logger.isDebugEnabled())
                    {
                        MuleMessage resultMessage = resultToReturn == null ? null : resultToReturn.getMessage();
                        logger.debug("Received final Chain result '" + i + "': "
                            + (resultMessage == null ? "null" : resultMessage.toString()));
                    }
                }
            }

        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(resultToReturn, endpoint, e);
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
     * @param intermediaryResult the message travelling across the targets
     */
    protected void processIntermediaryResult(MuleMessage localResult, MuleMessage intermediaryResult)
    {
        localResult.setCorrelationId(intermediaryResult.getCorrelationId());
        localResult.setCorrelationSequence(intermediaryResult.getCorrelationSequence());
        localResult.setCorrelationGroupSize(intermediaryResult.getCorrelationGroupSize());
        localResult.setReplyTo(intermediaryResult.getReplyTo());
    }
}
