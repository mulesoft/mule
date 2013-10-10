/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
