/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.config.spring.factories;

import org.mule.api.cache.CachingStrategy;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.config.spring.factories.AbstractFlowConstructFactoryBean;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.transport.http.construct.HttpProxy;
import org.mule.transport.http.construct.builder.HttpProxyBuilder;

public class HttpProxyFactoryBean extends AbstractFlowConstructFactoryBean
{

    final HttpProxyBuilder httpProxyBuilder = new HttpProxyBuilder();

    public Class<?> getObjectType()
    {
        return HttpProxy.class;
    }

    @Override
    protected AbstractFlowConstructBuilder<HttpProxyBuilder, HttpProxy> getFlowConstructBuilder()
    {
        return httpProxyBuilder;
    }

    public void setEndpoint(final OutboundEndpoint endpoint)
    {
        httpProxyBuilder.outboundEndpoint(endpoint);
    }

    public void setMessageProcessor(final MessageProcessor processor)
    {
        httpProxyBuilder.outboundEndpoint((OutboundEndpoint) processor);
    }

    public void setInboundAddress(final String inboundAddress)
    {
        httpProxyBuilder.inboundAddress(inboundAddress);
    }

    public void setInboundEndpoint(final EndpointBuilder inboundEndpointBuilder)
    {
        httpProxyBuilder.inboundEndpoint(inboundEndpointBuilder);
    }

    public void setOutboundAddress(final String outboundAddress)
    {
        httpProxyBuilder.outboundAddress(outboundAddress);
    }

    public void setOutboundEndpoint(final EndpointBuilder outboundEndpointBuilder)
    {
        httpProxyBuilder.outboundEndpoint(outboundEndpointBuilder);
    }

    public void setTransformers(final Transformer... transformers)
    {
        httpProxyBuilder.transformers(transformers);
    }

    public void setResponseTransformers(final Transformer... responseTransformers)
    {
        httpProxyBuilder.responseTransformers(responseTransformers);
    }

    public void setCachingStrategy(final CachingStrategy cachingStrategy)
    {
        httpProxyBuilder.cachingStrategy(cachingStrategy);
    }
}
