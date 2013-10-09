/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.construct.builder;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.cache.CachingStrategy;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.builder.AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder;
import org.mule.transport.http.construct.HttpProxy;

import java.util.Arrays;

public class HttpProxyBuilder extends
    AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<HttpProxyBuilder, HttpProxy>
{
    private CachingStrategy cachingStrategy;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    @Override
    protected MessageExchangePattern getOutboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    public HttpProxyBuilder transformers(final Transformer... outboundTransformers)
    {
        this.transformers = Arrays.asList((MessageProcessor[]) outboundTransformers);
        return this;
    }

    public HttpProxyBuilder responseTransformers(final Transformer... outboundResponseTransformers)
    {
        this.responseTransformers = Arrays.asList((MessageProcessor[]) outboundResponseTransformers);
        return this;
    }

    public HttpProxyBuilder cachingStrategy(final CachingStrategy cachingStrategy)
    {
        this.cachingStrategy = cachingStrategy;
        return this;
    }

    @Override
    protected HttpProxy buildFlowConstruct(final MuleContext muleContext) throws MuleException
    {
        return new HttpProxy(name, muleContext, getOrBuildInboundEndpoint(muleContext),
            getOrBuildOutboundEndpoint(muleContext), transformers, responseTransformers,
            cachingStrategy);
    }
}
