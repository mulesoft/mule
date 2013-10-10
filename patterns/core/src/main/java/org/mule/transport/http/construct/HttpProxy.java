/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.AbstractConfigurationPattern;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.pattern.core.support.CopyInboundToOutboundPropertiesTransformerCallback;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.TransformerTemplate.TransformerCallback;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;

import java.util.List;

/**
 * A simple HTTP proxy that supports transformation and caching.
 */
public class HttpProxy extends AbstractConfigurationPattern
{
    private final OutboundEndpoint outboundEndpoint;

    private final InterceptingMessageProcessor cachingMessageProcessor;

    public HttpProxy(final String name,
                     final MuleContext muleContext,
                     final MessageSource messageSource,
                     final OutboundEndpoint outboundEndpoint,
                     final List<MessageProcessor> transformers,
                     final List<MessageProcessor> responseTransformers,
                     final InterceptingMessageProcessor cachingMessageProcessor) throws MuleException
    {
        super(name, muleContext, transformers, responseTransformers);

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
        this.cachingMessageProcessor = cachingMessageProcessor;
    }

    @Override
    protected void configureMessageProcessorsBeforeTransformation(final MessageProcessorChainBuilder builder)
    {
        configureContentLengthRemover(this, builder);
    }

    @Override
    protected void configureMessageProcessorsAfterTransformation(final MessageProcessorChainBuilder builder)
        throws MuleException
    {
        // ensure properties, hence HTTP headers, are propagated both ways
        final TransformerTemplate copyInboundToOutboundPropertiesTransformer = new TransformerTemplate(
            new CopyInboundToOutboundPropertiesTransformerCallback());
        builder.chain(copyInboundToOutboundPropertiesTransformer);
        builder.chain(new ResponseMessageProcessorAdapter(copyInboundToOutboundPropertiesTransformer));

        if (cachingMessageProcessor != null)
        {
            builder.chain(cachingMessageProcessor);
        }

        if (outboundEndpoint instanceof DynamicURIOutboundEndpoint)
        {
            // do not mess with endpoints that are already dynamic
            builder.chain(outboundEndpoint);
        }
        else
        {
            // create a templated outbound endpoint to propagate extra path elements (including query parameters)
            builder.chain(new TransformerTemplate(new TransformerCallback()
            {
                public Object doTransform(final MuleMessage message) throws Exception
                {
                    final String pathExtension = StringUtils.substringAfter(
                        (String) message.getInboundProperty("http.request"),
                        (String) message.getInboundProperty("http.context.path"));

                    message.setInvocationProperty("http.path.extension",
                        StringUtils.defaultString(pathExtension));
                    return message;
                }
            }));

            logger.error("Endpoint: " + outboundEndpoint);
            final DynamicOutboundEndpoint dynamicOutboundEndpoint;

            if (outboundEndpoint instanceof DynamicOutboundEndpoint)
            {
                dynamicOutboundEndpoint = (DynamicOutboundEndpoint) outboundEndpoint;
            }
            else
            {
                final String uriTemplate = outboundEndpoint.getEndpointURI().getUri().toString()
                                           + "#[variable:http.path.extension]";

                dynamicOutboundEndpoint = new DynamicOutboundEndpoint(
                        new EndpointURIEndpointBuilder(outboundEndpoint), uriTemplate);
            }

            builder.chain(dynamicOutboundEndpoint);
        }
    }

    public static void configureContentLengthRemover(final AbstractConfigurationPattern configurationPattern,
                                                     final MessageProcessorChainBuilder builder)
    {
        // if transformers have been configured, preemptively drop the content-length header to prevent side effects
        // induced by mismatches
        if ((configurationPattern.hasTransformers()) || (configurationPattern.hasResponseTransformers()))
        {
            final MessagePropertiesTransformer contentLengthHeaderRemover = newContentLengthHeaderRemover();
            if (configurationPattern.hasTransformers())
            {
                builder.chain(contentLengthHeaderRemover);
            }
            if (configurationPattern.hasResponseTransformers())
            {
                builder.chain(new ResponseMessageProcessorAdapter(contentLengthHeaderRemover));
            }
        }
    }

    private static MessagePropertiesTransformer newContentLengthHeaderRemover()
    {
        final MessagePropertiesTransformer contentLengthHeaderRemover = new MessagePropertiesTransformer();
        contentLengthHeaderRemover.setScope(PropertyScope.INBOUND);
        contentLengthHeaderRemover.setDeleteProperties("(?i)content-length");
        return contentLengthHeaderRemover;
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if (messageSource instanceof InboundEndpoint)
        {
            final InboundEndpoint inboundEndpoint = (InboundEndpoint) messageSource;

            if (!inboundEndpoint.getExchangePattern().equals(MessageExchangePattern.REQUEST_RESPONSE))
            {
                throw new FlowConstructInvalidException(
                    MessageFactory.createStaticMessage("HttpProxy only works with a request-response inbound endpoint."),
                    this);
            }
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
