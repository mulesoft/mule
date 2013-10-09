/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
