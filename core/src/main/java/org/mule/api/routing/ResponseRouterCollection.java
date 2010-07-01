/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;

import java.util.List;

/**
 * <code>ResponseRouterCollection</code> is a router that can be used to control how
 * the response in a request/response message flow is created. Its main use case is
 * to aggregate a set of asynchonous events into a single response.
 */

public interface ResponseRouterCollection extends RouterCollection, MessageProcessor
{
    void addRouter(InboundRouter router);

    InboundRouter removeRouter(InboundRouter router);

    void addEndpoint(InboundEndpoint endpoint) throws MuleException;

    boolean removeEndpoint(InboundEndpoint endpoint) throws MuleException;

    List<InboundEndpoint> getEndpoints();

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see InboundRouterCollection
     */
    InboundEndpoint getEndpoint(String name);

    void setEndpoints(List<InboundEndpoint> endpoints) throws MuleException;

    MuleEvent getResponse(MuleEvent message) throws MuleException;

    int getTimeout();

    void setTimeout(int timeout);

    boolean hasEndpoints();
}
