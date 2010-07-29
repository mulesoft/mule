/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.construct.builder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.module.ws.construct.WSProxy;
import org.mule.util.FileUtils;

import edu.emory.mathcs.backport.java.util.Arrays;

public class WSProxyBuilder extends AbstractFlowConstructBuilder<WSProxyBuilder, WSProxy>
{
    protected OutboundEndpoint outboundEndpoint;
    protected EndpointBuilder outboundEndpointBuilder;
    protected String outboundAddress;
    protected URI wsldLocation;
    protected File wsdlFile;

    // pull-up if/when needed
    protected List<MessageProcessor> outboundTransformers;
    protected List<MessageProcessor> outboundResponseTransformers;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    public WSProxyBuilder outboundEndpoint(OutboundEndpoint outboundEndpoint)
    {
        this.outboundEndpoint = outboundEndpoint;
        return this;
    }

    public WSProxyBuilder outboundEndpoint(EndpointBuilder outboundEndpointBuilder)
    {
        this.outboundEndpointBuilder = outboundEndpointBuilder;
        return this;
    }

    public WSProxyBuilder outboundAddress(String outboundAddress)
    {
        this.outboundAddress = outboundAddress;
        return this;
    }

    @SuppressWarnings("unchecked")
    public WSProxyBuilder outboundTransformers(Transformer... outboundTransformers)
    {
        this.outboundTransformers = Arrays.asList(outboundTransformers);
        return this;
    }

    @SuppressWarnings("unchecked")
    public WSProxyBuilder outboundResponseTransformers(Transformer... outboundResponseTransformers)
    {
        this.outboundResponseTransformers = Arrays.asList(outboundResponseTransformers);
        return this;
    }

    public WSProxyBuilder wsldLocation(URI wsldLocation)
    {
        this.wsldLocation = wsldLocation;
        return this;
    }

    public WSProxyBuilder wsdlFile(File wsdlFile)
    {
        this.wsdlFile = wsdlFile;
        return this;
    }

    @Override
    protected WSProxy buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        if (wsdlFile != null)
        {
            return buildStaticWsdlContentsWSProxy(muleContext);
        }

        if (wsldLocation != null)
        {
            return buildStaticWsdlUriWSProxy(muleContext);
        }

        return buildDynamicWsdlUriWSProxy(muleContext);
    }

    private WSProxy buildDynamicWsdlUriWSProxy(MuleContext muleContext) throws MuleException
    {
        return new WSProxy(muleContext, name, buildMessageSource(muleContext),
            buildOrGetOutboundEndpoint(muleContext));
    }

    private WSProxy buildStaticWsdlContentsWSProxy(MuleContext muleContext) throws MuleException
    {
        try
        {
            return new WSProxy(muleContext, name, buildMessageSource(muleContext),
                buildOrGetOutboundEndpoint(muleContext), FileUtils.readFileToString(wsdlFile));
        }
        catch (final IOException ioe)
        {
            throw new ConfigurationException(ioe);
        }
    }

    private WSProxy buildStaticWsdlUriWSProxy(MuleContext muleContext) throws MuleException
    {
        return new WSProxy(muleContext, name, buildMessageSource(muleContext),
            buildOrGetOutboundEndpoint(muleContext), wsldLocation);
    }

    protected OutboundEndpoint buildOrGetOutboundEndpoint(MuleContext muleContext) throws MuleException
    {
        if (outboundEndpoint != null)
        {
            return outboundEndpoint;
        }

        if (outboundEndpointBuilder == null)
        {
            outboundEndpointBuilder = muleContext.getRegistry().lookupEndpointFactory().getEndpointBuilder(
                outboundAddress);
        }

        outboundEndpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

        if (outboundTransformers != null)
        {
            outboundEndpointBuilder.setMessageProcessors(outboundTransformers);
        }

        if (outboundResponseTransformers != null)
        {
            outboundEndpointBuilder.setResponseMessageProcessors(outboundResponseTransformers);
        }

        return outboundEndpointBuilder.buildOutboundEndpoint();
    }

}
