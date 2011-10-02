/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointCache;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache endpoints in order to prevent memory leaks.
 *
 * see MULE-5422
 */
public class SimpleEndpointCache implements EndpointCache
{
    protected MuleContext muleContext;
    private ConcurrentMap inboundEndpointCache = new ConcurrentHashMap();
    private ConcurrentMap outboundEndpointCache = new ConcurrentHashMap();

    public SimpleEndpointCache(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public OutboundEndpoint getOutboundEndpoint(String uri,
                                                   MessageExchangePattern mep,
                                                   Long responseTimeout) throws MuleException
    {
        OutboundEndpoint endpoint = (OutboundEndpoint) outboundEndpointCache.get(uri + ":" + mep.toString()
                                                                                 + ":" + responseTimeout);
        if (endpoint == null)
        {
            EndpointBuilder endpointBuilder = muleContext.getEndpointFactory()
                .getEndpointBuilder(uri);
            endpointBuilder.setExchangePattern(mep);
            if (responseTimeout != null && responseTimeout > 0)
            {
                endpointBuilder.setResponseTimeout(responseTimeout.intValue());
            }
            endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
            OutboundEndpoint concurrentlyAddedEndpoint = (OutboundEndpoint) outboundEndpointCache.putIfAbsent(
                uri + ":" + mep.toString() + ":" + responseTimeout, endpoint);
            if (concurrentlyAddedEndpoint != null)
            {
                return concurrentlyAddedEndpoint;
            }
        }
        return endpoint;
    }

    public InboundEndpoint getInboundEndpoint(String uri, MessageExchangePattern mep) throws MuleException
    {
        InboundEndpoint endpoint = (InboundEndpoint) inboundEndpointCache.get(uri + ":" + mep.toString());
        if (endpoint == null)
        {
            EndpointBuilder endpointBuilder = muleContext.getEndpointFactory()
                .getEndpointBuilder(uri);
            endpointBuilder.setExchangePattern(mep);
            endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointBuilder);
            InboundEndpoint concurrentlyAddedEndpoint = (InboundEndpoint) inboundEndpointCache.putIfAbsent(
                uri + ":" + mep.toString(), endpoint);
            if (concurrentlyAddedEndpoint != null)
            {
                return concurrentlyAddedEndpoint;
            }
        }
        return endpoint;
    }
}
