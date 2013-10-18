/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;

/**
 * A simple bridge between a single inbound endpoint and a single outbound endpoint. It enforces a consistent exchange
 * pattern across its endpoints. If declared transactional, it ensures that the correct endpoint configuration is in
 * place.
 */
public class Bridge extends AbstractConfigurationPattern
{
    private final OutboundEndpoint outboundEndpoint;
    private final MessageExchangePattern exchangePattern;
    private final boolean transacted;

    public Bridge(String name,
                  MuleContext muleContext,
                  MessageSource messageSource,
                  OutboundEndpoint outboundEndpoint,
                  List<MessageProcessor> transformers,
                  List<MessageProcessor> responseTransformers,
                  MessageExchangePattern exchangePattern,
                  boolean transacted)
    {
        super(name, muleContext, transformers, responseTransformers);

        Validate.notNull(messageSource, "messageSource can't be null");
        Validate.notNull(outboundEndpoint, "outboundEndpoint can't be null");
        Validate.notNull(exchangePattern, "exchangePattern can't be null");

        this.messageSource = messageSource;
        this.outboundEndpoint = outboundEndpoint;
        this.exchangePattern = exchangePattern;
        this.transacted = transacted;
    }

    @Override
    protected void configureMessageProcessorsBeforeTransformation(MessageProcessorChainBuilder builder)
    {
        // NOOP
    }

    @Override
    protected void configureMessageProcessorsAfterTransformation(MessageProcessorChainBuilder builder)
    {
        builder.chain(outboundEndpoint);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if (messageSource instanceof InboundEndpoint)
        {
            validateInboundEndpoint((InboundEndpoint) messageSource);
        }

        validateOutboundEndpoint();
    }

    private void validateOutboundEndpoint() throws FlowConstructInvalidException
    {
        if (outboundEndpoint.getExchangePattern() != exchangePattern)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Inconsistent bridge outbound endpoint exchange pattern, expected "
                                                   + exchangePattern
                                                   + " but was "
                                                   + outboundEndpoint.getExchangePattern()), this);
        }

        if (transacted
            && ((outboundEndpoint.getTransactionConfig() == null) || (!outboundEndpoint.getTransactionConfig()
                .isConfigured())))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("A transacted bridge requires a transacted outbound endpoint"),
                this);
        }

        if ((!transacted) && (outboundEndpoint.getTransactionConfig() != null)
            && (outboundEndpoint.getTransactionConfig().isConfigured()))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("A non-transacted bridge requires a non-transacted outbound endpoint"),
                this);
        }
    }

    private void validateInboundEndpoint(InboundEndpoint inboundEndpoint)
        throws FlowConstructInvalidException
    {
        if (inboundEndpoint.getExchangePattern() != exchangePattern)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Inconsistent bridge inbound endpoint exchange pattern, expected "
                                                   + exchangePattern
                                                   + " but was "
                                                   + inboundEndpoint.getExchangePattern()), this);
        }

        if (transacted
            && ((inboundEndpoint.getTransactionConfig() == null) || (!inboundEndpoint.getTransactionConfig()
                .isConfigured())))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("A transacted bridge requires a transacted inbound endpoint"),
                this);
        }

        if ((!transacted) && (inboundEndpoint.getTransactionConfig() != null)
            && (inboundEndpoint.getTransactionConfig().isConfigured()))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("A non-transacted bridge requires a non-transacted inbound endpoint"),
                this);
        }
    }

    @Override
    public String getConstructType()
    {
        return "Bridge";
    }
}
