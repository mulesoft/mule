/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.apache.commons.lang.Validate;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.processor.FlowConstructStatisticsMessageObserver;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.routing.ChoiceRouter;

public class Validator extends AbstractFlowConstruct
{
    private final OutboundEndpoint outboundEndpoint;
    private final Filter validationFilter;
    private final String ackExpression;
    private final String nackExpression;

    public Validator(String name,
                     MuleContext muleContext,
                     MessageSource messageSource,
                     OutboundEndpoint outboundEndpoint,
                     Filter validationFilter,
                     String ackExpression,
                     String nackExpression)
    {
        super(name, muleContext);

        Validate.notNull(messageSource, "messageSource can't be null");
        Validate.notNull(outboundEndpoint, "outboundEndpoint can't be null");
        Validate.notNull(validationFilter, "validationFilter can't be null");
        Validate.notEmpty(ackExpression, "ackExpression can't be empty");
        Validate.notEmpty(nackExpression, "nackExpression can't be empty");

        this.messageSource = messageSource;
        this.outboundEndpoint = outboundEndpoint;
        this.validationFilter = validationFilter;
        this.ackExpression = ackExpression;
        this.nackExpression = nackExpression;
    }

    @Override
    protected void configureMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageObserver());

        EventReturningMessageProcessor outboundMessageProcessor = new EventReturningMessageProcessor();
        outboundMessageProcessor.setListener(outboundEndpoint);

        ResponseMessageProcessorAdapter ackResponseMessageProcessor = new ResponseMessageProcessorAdapter();
        ackResponseMessageProcessor.setListener(outboundMessageProcessor);
        ackResponseMessageProcessor.setProcessor(getExpressionTransformer(getName() + "-ack-expression",
            ackExpression));

        // a simple success/failure choice router determines which response to return
        final ChoiceRouter choiceRouter = new ChoiceRouter();
        choiceRouter.addRoute(ackResponseMessageProcessor, validationFilter);
        choiceRouter.setDefaultRoute(getExpressionTransformer(getName() + "-nack-expression", nackExpression));
        builder.chain(choiceRouter);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        validateMessageSource();
        validateOutboundEndpoint();
        validateExpression(ackExpression);
        validateExpression(nackExpression);
    }

    private void validateMessageSource() throws FlowConstructInvalidException
    {
        if ((messageSource instanceof InboundEndpoint)
            && (!((InboundEndpoint) messageSource).getExchangePattern().equals(
                MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Validator only works with a request-response inbound endpoint."),
                this);
        }
    }

    private void validateOutboundEndpoint() throws FlowConstructInvalidException
    {
        if (!outboundEndpoint.getExchangePattern().equals(MessageExchangePattern.ONE_WAY))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Validator only works with a one-way outbound endpoint."),
                this);
        }
    }

    private void validateExpression(String expression) throws FlowConstructInvalidException
    {
        if (!muleContext.getExpressionManager().isExpression(expression))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Invalid expression in Validator: " + expression), this);
        }
    }

    private ExpressionTransformer getExpressionTransformer(String name, String expression)
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.parse(expression);

        ExpressionArgument expressionArgument = new ExpressionArgument(name, expressionConfig, false);
        expressionArgument.setMuleContext(muleContext);

        ExpressionTransformer expressionTransformer = new ExpressionTransformer();
        expressionTransformer.setMuleContext(muleContext);
        expressionTransformer.addArgument(expressionArgument);

        try
        {
            expressionTransformer.initialise();
        }
        catch (InitialisationException ie)
        {
            throw new MuleRuntimeException(ie);
        }

        return expressionTransformer;
    }

    private static class EventReturningMessageProcessor extends AbstractInterceptingMessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            super.processNext(event);
            return event;
        }

    }
}
