/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.MessageExchangePattern;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Bridge;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.construct.builder.BridgeBuilder;

public class BridgeFactoryBean extends AbstractFlowConstructFactoryBean
{
    final BridgeBuilder bridgeBuilder = new BridgeBuilder();

    public Class<?> getObjectType()
    {
        return Bridge.class;
    }

    @Override
    protected AbstractFlowConstructBuilder<BridgeBuilder, Bridge> getFlowConstructBuilder()
    {
        return bridgeBuilder;
    }
    
    public void setEndpoint(OutboundEndpoint endpoint)
    {
        bridgeBuilder.outboundEndpoint(endpoint);
    }
    
    public void setMessageProcessor(MessageProcessor processor)
    {
        bridgeBuilder.outboundEndpoint((OutboundEndpoint) processor);
    }

    public void setInboundAddress(String inboundAddress)
    {
        bridgeBuilder.inboundAddress(inboundAddress);
    }

    public void setInboundEndpoint(EndpointBuilder inboundEndpointBuilder)
    {
        bridgeBuilder.inboundEndpoint(inboundEndpointBuilder);
    }

    public void setOutboundAddress(String outboundAddress)
    {
        bridgeBuilder.outboundAddress(outboundAddress);
    }

    public void setOutboundEndpoint(EndpointBuilder outboundEndpointBuilder)
    {
        bridgeBuilder.outboundEndpoint(outboundEndpointBuilder);
    }

    public void setTransformers(Transformer... transformers)
    {
        bridgeBuilder.transformers(transformers);
    }

    public void setResponseTransformers(Transformer... responseTransformers)
    {
        bridgeBuilder.responseTransformers(responseTransformers);
    }

    public void setTransacted(boolean transacted)
    {
        bridgeBuilder.transacted(transacted);
    }

    public void setExchangePattern(MessageExchangePattern exchangePattern)
    {
        bridgeBuilder.exchangePattern(exchangePattern);
    }
}
