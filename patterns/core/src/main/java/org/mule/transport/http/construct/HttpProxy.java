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

import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.AbstractConfigurationPattern;
import org.mule.pattern.core.construct.CopyInboundToOutboundPropertiesTransformerCallback;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.util.ObjectUtils;

/**
 * A simple HTTP proxy that supports transformation and caching.
 */
public class HttpProxy extends AbstractConfigurationPattern
{
    // TODO (DDO) support outbound request path extension
    // TODO (DDO) support caching, using SimpleCachingHeadersPageCachingFilter / ObjectStore / mule-module-cache
    // TODO (DDO) support cache bypass? X-Mule-HttpProxy-CacheControl=no-cache?

    private final OutboundEndpoint outboundEndpoint;

    public HttpProxy(final String name,
                     final MuleContext muleContext,
                     final MessageSource messageSource,
                     final OutboundEndpoint outboundEndpoint,
                     final List<MessageProcessor> transformers,
                     final List<MessageProcessor> responseTransformers) throws MuleException
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
    }

    @Override
    protected void configureMessageProcessorsBeforeTransformation(final MessageProcessorChainBuilder builder)
    {
        // if transformers have been configured, pre-emptively drop the content-length header to prevent side effects
        // induced by mismatches
        if ((hasTransformers()) || (hasResponseTransformers()))
        {
            final MessagePropertiesTransformer contentLengthHeaderRemover = newContentLengthHeaderRemover();
            if (hasTransformers())
            {
                builder.chain(contentLengthHeaderRemover);
            }
            if (hasResponseTransformers())
            {
                builder.chain(new ResponseMessageProcessorAdapter(contentLengthHeaderRemover));
            }
        }
    }

    public static MessagePropertiesTransformer newContentLengthHeaderRemover()
    {
        final MessagePropertiesTransformer contentLengthHeaderRemover = new MessagePropertiesTransformer();
        contentLengthHeaderRemover.setScope(PropertyScope.INBOUND);
        contentLengthHeaderRemover.setDeleteProperties("(?i)content-length");
        return contentLengthHeaderRemover;
    }

    @Override
    protected void configureMessageProcessorsAfterTransformation(final MessageProcessorChainBuilder builder)
    {
        // ensure properties, hence HTTP headers, are propagated both ways
        final TransformerTemplate copyInboundToOutboundPropertiesTransformer = new TransformerTemplate(
            new CopyInboundToOutboundPropertiesTransformerCallback());
        builder.chain(copyInboundToOutboundPropertiesTransformer);
        builder.chain(new ResponseMessageProcessorAdapter(copyInboundToOutboundPropertiesTransformer));

        builder.chain(outboundEndpoint);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        // FIXME (DDO) enforce HTTP endpoints

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
