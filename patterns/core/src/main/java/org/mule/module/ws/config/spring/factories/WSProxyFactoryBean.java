/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.config.spring.factories;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.config.spring.factories.AbstractFlowConstructFactoryBean;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.module.ws.construct.WSProxy;
import org.mule.module.ws.construct.builder.WSProxyBuilder;

import java.io.File;
import java.net.URI;

public class WSProxyFactoryBean extends AbstractFlowConstructFactoryBean
{
    final WSProxyBuilder wsProxyBuilder = new WSProxyBuilder();

    public Class<?> getObjectType()
    {
        return WSProxy.class;
    }

    @Override
    protected AbstractFlowConstructBuilder<WSProxyBuilder, WSProxy> getFlowConstructBuilder()
    {
        return wsProxyBuilder;
    }

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        wsProxyBuilder.outboundEndpoint(endpoint);
    }
    
    public void setMessageProcessor(MessageProcessor processor)
    {
        wsProxyBuilder.outboundEndpoint((OutboundEndpoint) processor);
    }

    public void setInboundAddress(String inboundAddress)
    {
        wsProxyBuilder.inboundAddress(inboundAddress);
    }

    public void setInboundEndpoint(EndpointBuilder inboundEndpointBuilder)
    {
        wsProxyBuilder.inboundEndpoint(inboundEndpointBuilder);
    }

    public void setOutboundAddress(String outboundAddress)
    {
        wsProxyBuilder.outboundAddress(outboundAddress);
    }

    public void setOutboundEndpoint(EndpointBuilder outboundEndpointBuilder)
    {
        wsProxyBuilder.outboundEndpoint(outboundEndpointBuilder);
    }

    public void setTransformers(Transformer... transformers)
    {
        wsProxyBuilder.transformers(transformers);
    }

    public void setResponseTransformers(Transformer... responseTransformers)
    {
        wsProxyBuilder.responseTransformers(responseTransformers);
    }

    public void setWsdlLocation(URI wsldLocation)
    {
        wsProxyBuilder.wsldLocation(wsldLocation);
    }

    public void setWsdlFile(File wsdlFile)
    {
        wsProxyBuilder.wsdlFile(wsdlFile);
    }
}
