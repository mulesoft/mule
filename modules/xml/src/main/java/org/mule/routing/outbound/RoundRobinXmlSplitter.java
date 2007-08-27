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

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * This router will split the Xml message into parts based on the xpath expression
 * and route each new event to the endpoints on the router, one after the other.
 */
public class RoundRobinXmlSplitter extends FilteringXmlMessageSplitter
{
    // We have to do some additional checks if we're going to allow filters on the
    // round robin endpoints
    // So for performance lets turn it off by default
    protected volatile boolean enableEndpointFiltering = false;
    private boolean deterministic = true;
    private static final AtomicInteger globalCounter = new AtomicInteger(0);

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        try
        {
            String correlationId = (String)propertyExtractor.getProperty(
                MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
            initialise(message);

            UMOEndpoint endpoint;
            UMOMessage result = null;
            Document part;
            List parts = (List)nodesContext.get();
            if (parts == null)
            {
                logger.error("There are no parts for current message. No events were routed: " + message);
                return null;
            }
            int correlationSequence = 1;
            Counter epCounter = new Counter();
            Iterator iterator = parts.iterator();
            while (iterator.hasNext())
            {
                part = (Document)iterator.next();
                // Create the message
                Map theProperties = (Map)propertiesContext.get();
                message = new MuleMessage(part, new HashMap(theProperties));

                if (enableEndpointFiltering)
                {
                    endpoint = getEndpointForMessage(message);
                }
                else
                {
                    endpoint = (UMOEndpoint)getEndpoints().get(epCounter.next());
                }

                if (endpoint == null)
                {
                    logger.error("There was no matching endpoint for message part: " + part.asXML());
                }
                else
                {
                    try
                    {
                        if (enableCorrelation != ENABLE_CORRELATION_NEVER)
                        {
                            boolean correlationSet = message.getCorrelationId() != null;
                            if (!correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
                            {
                                message.setCorrelationId(correlationId);
                            }

                            // take correlation group size from the message
                            // properties, set by concrete message splitter
                            // implementations
                            final int groupSize = message.getCorrelationGroupSize();
                            message.setCorrelationGroupSize(groupSize);
                            message.setCorrelationSequence(correlationSequence++);
                        }
                        if (synchronous)
                        {
                            result = send(session, message, endpoint);
                        }
                        else
                        {
                            dispatch(session, message, endpoint);
                        }
                    }
                    catch (UMOException e)
                    {
                        throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
                    }
                }
            }
            return result;
        }
        finally
        {
            nodesContext.set(null);
            propertiesContext.set(null);
        }
    }

    /**
     * Retrieves a specific message part for the given endpoint. the message will
     * then be routed via the provider.
     * 
     * @param message the current message being processed
     * @return the message part to dispatch
     */
    protected UMOEndpoint getEndpointForMessage(UMOMessage message)
    {
        for (int i = 0; i < endpoints.size(); i++)
        {
            UMOEndpoint endpoint = (UMOEndpoint)endpoints.get(i);

            try
            {
                if (endpoint.getFilter() == null || endpoint.getFilter().accept(message))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Endpoint filter matched for node " + i + ". Routing message over: "
                                     + endpoint.getEndpointURI().toString());
                    }
                    return endpoint;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Endpoint filter did not match");
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Unable to create message for node at position " + i, e);
                return null;
            }
        }

        return null;
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint.getFilter() != null && !enableEndpointFiltering)
        {
            throw new IllegalStateException(
                "Endpoints on the RoundRobin splitter router cannot have filters associated with them");
        }
        super.addEndpoint(endpoint);
    }

    public boolean isEnableEndpointFiltering()
    {
        return enableEndpointFiltering;
    }

    public void setEnableEndpointFiltering(boolean enableEndpointFiltering)
    {
        this.enableEndpointFiltering = enableEndpointFiltering;
    }

    public boolean isDeterministic()
    {
        return deterministic;
    }

    public void setDeterministic(boolean deterministic)
    {
        this.deterministic = deterministic;
    }

    private class Counter
    {

        private AtomicInteger counter;

        public Counter()
        {
            if (isDeterministic())
            {
                counter = new AtomicInteger(0);
            }
            else
            {
                counter = globalCounter;
            }
        }

        public int next()
        {
            return counter.getAndIncrement() % getEndpoints().size();
        }

    }

}
