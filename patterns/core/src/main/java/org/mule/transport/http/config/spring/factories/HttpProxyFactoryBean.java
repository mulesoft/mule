/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
