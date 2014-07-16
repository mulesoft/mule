/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import java.util.Collections;

import org.apache.commons.lang.Validate;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.routing.filter.Filter;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.exception.MessagingExceptionHandlerToSystemAdapter;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.routing.ChoiceRouter;
import org.mule.util.StringUtils;

public class Validator extends AbstractConfigurationPattern
{
    private final OutboundEndpoint outboundEndpoint;
    private final Filter validationFilter;
    private final String ackExpression;
    private final String nackExpression;
    private final String errorExpression;

    public Validator(String name,
                     MuleContext muleContext,
                     MessageSource messageSource,
                     OutboundEndpoint outboundEndpoint,
                     Filter validationFilter,
                     String ackExpression,
                     String nackExpression)
    {
        this(name, muleContext, messageSource, outboundEndpoint, validationFilter, ackExpression,
            nackExpression, null);
    }

    @SuppressWarnings("unchecked")
    public Validator(String name,
                     MuleContext muleContext,
                     MessageSource messageSource,
                     OutboundEndpoint outboundEndpoint,
                     Filter validationFilter,
                     String ackExpression,
                     String nackExpression,
                     String errorExpression)
    {
        super(name, muleContext, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

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
        this.errorExpression = errorExpression;
    }

    @Override
    protected void configureMessageProcessorsBeforeTransformation(MessageProcessorChainBuilder builder)
    {
        // NOOP
    }

    @Override
    protected void configureMessageProcessorsAfterTransformation(MessageProcessorChainBuilder builder)
    {
        final ErrorAwareEventReturningMessageProcessor outboundMessageProcessor = new ErrorAwareEventReturningMessageProcessor();
        outboundMessageProcessor.setListener(outboundEndpoint);

        final ResponseMessageProcessorAdapter ackResponseMessageProcessor = new ResponseMessageProcessorAdapter();
        ackResponseMessageProcessor.setListener(outboundMessageProcessor);
        ackResponseMessageProcessor.setProcessor(getExpressionTransformer(getName() + "-ack-expression",
                                                                          ackExpression));

        MessageProcessor validRouteMessageProcessor = ackResponseMessageProcessor;

        if (hasErrorExpression())
        {
            final ErrorExpressionTransformerMessageProcessor errorResponseMessageProcessor = new ErrorExpressionTransformerMessageProcessor(
                getExpressionTransformer(getName() + "-error-expression", errorExpression));
            errorResponseMessageProcessor.setListener(ackResponseMessageProcessor);
            validRouteMessageProcessor = errorResponseMessageProcessor;
        }

        // a simple success/failure choice router determines which response to return
        final ChoiceRouter choiceRouter = new ChoiceRouter();
        choiceRouter.addRoute(validRouteMessageProcessor, validationFilter);
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

        if (hasErrorExpression())
        {
            validateExpression(errorExpression);
        }
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
        if ((hasErrorExpression())
            && (!outboundEndpoint.getExchangePattern().equals(MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("Validator with an error expression only works with a request-response outbound endpoint."),
                this);
        }
    }

    protected boolean hasErrorExpression()
    {
        return StringUtils.isNotBlank(errorExpression);
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
        final ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.parse(expression);

        final ExpressionArgument expressionArgument = new ExpressionArgument(name, expressionConfig, false);
        expressionArgument.setMuleContext(muleContext);

        final ExpressionTransformer expressionTransformer = new ExpressionTransformer();
        expressionTransformer.setMuleContext(muleContext);
        expressionTransformer.addArgument(expressionArgument);

        try
        {
            expressionTransformer.initialise();
        }
        catch (final InitialisationException ie)
        {
            throw new MuleRuntimeException(ie);
        }

        return expressionTransformer;
    }

    private static class ErrorExpressionTransformerMessageProcessor extends
        AbstractInterceptingMessageProcessor
    {
        private final ExpressionTransformer errorExpressionTransformer;

        public ErrorExpressionTransformerMessageProcessor(ExpressionTransformer errorExpressionTransformer)
        {
            this.errorExpressionTransformer = errorExpressionTransformer;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            final MuleEvent nextResult = super.processNext(event);

            final ExceptionPayload nextResultMessageExceptionPayload = getExceptionPayload(nextResult);

            if (nextResultMessageExceptionPayload != null)
            {
                return errorExpressionTransformer.process(event);
            }

            return nextResult;
        }
    }

    private static class ErrorAwareEventReturningMessageProcessor extends
        AbstractInterceptingMessageProcessor
    {
        /*
         * Returns the incoming event whatever the outcome of the rest of the chain maybe. Sets an exception payload on
         * the incoming event if an error occurred downstream.
         */
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            final MuleEvent result = RequestContext.setEvent(event);

            try
            {
                final MuleEvent nextResult = super.processNext(event);

                final ExceptionPayload nextResultMessageExceptionPayload = getExceptionPayload(nextResult);

                if (nextResultMessageExceptionPayload != null)
                {
                    result.getMessage().setExceptionPayload(nextResultMessageExceptionPayload);
                }
            }
            catch (final MuleException me)
            {
                logger.error(me);
                result.getMessage().setExceptionPayload(new DefaultExceptionPayload(me));
            }

            return result;
        }
    }

    private static ExceptionPayload getExceptionPayload(MuleEvent event)
    {
        if (event == null || VoidMuleEvent.getInstance().equals(event))
        {
            return null;
        }

        final MuleMessage message = event.getMessage();
        if (message == null)
        {
            return null;
        }

        return message.getExceptionPayload();
    }

    @Override
    public String getConstructType()
    {
        return "Validator";
    }

    @Override
    protected void doInitialise() throws MuleException
    {
        if (outboundEndpoint instanceof MuleContextAware)
        {
            ((MuleContextAware) outboundEndpoint).setMuleContext(getMuleContext());
        }
        if (outboundEndpoint instanceof MessagingExceptionHandlerAware)
        {
            ((MessagingExceptionHandlerAware) outboundEndpoint).setMessagingExceptionHandler(new MessagingExceptionHandlerToSystemAdapter());
        }
        if (outboundEndpoint instanceof Initialisable)
        {
            ((Initialisable) outboundEndpoint).initialise();
        }
        super.doInitialise();
    }
}
