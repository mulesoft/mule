/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointCache;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;

import java.util.concurrent.Callable;

/**
 * Cache endpoints in order to prevent memory leaks.
 *
 * see MULE-5422
 */
public class SimpleEndpointCache implements EndpointCache
{
    protected MuleContext muleContext;

    private static final int MAX_SIZE = 1000;

    private Cache<String, InboundEndpoint> inboundEndpointCache =
            CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build();

    private Cache<String, OutboundEndpoint> outboundEndpointCache =
            CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build();

    public SimpleEndpointCache(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public OutboundEndpoint getOutboundEndpoint(final String uri,
                                                final MessageExchangePattern mep,
                                                final Long responseTimeout) throws MuleException
    {
        String key = uri + ":" + mep.toString() + ":" + responseTimeout;

        Callable<OutboundEndpoint> createAction = new Callable<OutboundEndpoint>()
        {
            @Override
            public OutboundEndpoint call() throws Exception
            {
                EndpointBuilder endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(uri);
                endpointBuilder.setExchangePattern(mep);
                if (responseTimeout != null && responseTimeout > 0)
                {
                    endpointBuilder.setResponseTimeout(responseTimeout.intValue());
                }
                return muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
            }
        };

        try
        {
            return outboundEndpointCache.get(key, createAction);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException("Error creating outbound endpoint", e);
        }
    }

    @Override
    public InboundEndpoint getInboundEndpoint(final String uri, final MessageExchangePattern mep) throws MuleException
    {
        String key = uri + ":" + mep.toString();

        Callable<InboundEndpoint> createAction = new Callable<InboundEndpoint>()
        {
            @Override
            public InboundEndpoint call() throws Exception
            {
                EndpointBuilder endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(uri);
                endpointBuilder.setExchangePattern(mep);
                return  muleContext.getEndpointFactory().getInboundEndpoint(endpointBuilder);
            }
        };

        try
        {
            return inboundEndpointCache.get(key, createAction);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException("Error creating inbound endpoint", e);
        }
    }
}
