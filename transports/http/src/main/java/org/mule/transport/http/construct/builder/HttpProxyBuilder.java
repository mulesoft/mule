/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
