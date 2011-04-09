/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.pattern.core.construct.CopyInboundToOutboundPropertiesTransformerCallback;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.StopFurtherMessageProcessingMessageProcessor;
import org.mule.transformer.TransformerTemplate;
import org.mule.util.ObjectUtils;

/**
 * A simple HTTP proxy that supports transformation and caching.
 */
public class HttpProxy extends AbstractFlowConstruct
{
    // TODO (DDO) support caching

    private final OutboundEndpoint outboundEndpoint;

    public HttpProxy(final String name,
                     final MuleContext muleContext,
                     final MessageSource messageSource,
                     final OutboundEndpoint outboundEndpoint) throws MuleException
    {
        super(name, muleContext);

        if (messageSource == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("messageSource can't be null on: " + this.toString()),
                this);
        }

        super.setMessageSource(messageSource);

        if (outboundEndpoint == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("outboundEndpoint can't be null on: " + this.toString()),
                this);
        }

        this.outboundEndpoint = outboundEndpoint;
    }

    @Override
    protected void configureMessageProcessors(final MessageProcessorChainBuilder builder)
    {
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());
        builder.chain(new StopFurtherMessageProcessingMessageProcessor());

        final TransformerTemplate copyInboundToOutboundPropertiesTransformer = new TransformerTemplate(
            new CopyInboundToOutboundPropertiesTransformerCallback());
        builder.chain(copyInboundToOutboundPropertiesTransformer);
        builder.chain(new ResponseMessageProcessorAdapter(copyInboundToOutboundPropertiesTransformer));

        // FIXME (DDO) MULE-5502 ensure outbound content length is correct when a transformer is defined on the proxy

        builder.chain(outboundEndpoint);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if ((messageSource instanceof InboundEndpoint)
            && (!((InboundEndpoint) messageSource).getExchangePattern().equals(
                MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("HttpProxy only works with a request-response inbound endpoint."),
                this);
        }

        if (!outboundEndpoint.getExchangePattern().equals(MessageExchangePattern.REQUEST_RESPONSE))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("HttpProxy only works with a request-response outbound endpoint."),
                this);
        }
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    @Override
    public String getConstructType()
    {
        return "HTTP-Proxy";
    }
}
