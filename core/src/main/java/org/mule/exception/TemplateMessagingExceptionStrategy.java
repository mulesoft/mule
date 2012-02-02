/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.requestreply.ReplyToPropertyRequestReplyReplier;

public abstract class TemplateMessagingExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor
{

    private MessageProcessorChain configuredMessageProcessors;
    private MessageProcessor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();
    private String expression;

    final public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        fireNotification(exception);
        logException(exception);
        processStatistics(event);
        event = beforeRouting(exception, event);
        event = route(event, exception);
        processOutboundRouterStatistics(event);
        event = afterRouting(exception, event);
        processReplyTo(event);
        closeStream(event.getMessage());
        nullifyExceptionPayloadIfRequired(event);
        return event;
    }

    protected void processReplyTo(MuleEvent event)
    {
        try
        {
            replyToMessageProcessor.process(event);
        }
        catch (MuleException e)
        {
            logFatal(event,e);
        }
    }

    protected abstract void nullifyExceptionPayloadIfRequired(MuleEvent event);

    private void processStatistics(MuleEvent event)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incExecutionError();
        }
    }

    protected MuleEvent route(MuleEvent event, Throwable t)
    {
        if (!getMessageProcessors().isEmpty())
        {
            try
            {
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(t));
                MuleEvent result = configuredMessageProcessors.process(event);
                processOutboundRouterStatistics(event);
                return result;
            }
            catch (Exception e)
            {
                logFatal(event, e);
            }
        }
        return event;
    }

    @Override
    protected void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        super.doInitialise(muleContext);
        DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder(this.flowConstruct);
        try
        {
            configuredMessageProcessors = defaultMessageProcessorChainBuilder.chain(getMessageProcessors()).build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }


    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public boolean accept(MuleEvent event)
    {
        return acceptsAll() || muleContext.getExpressionManager().evaluateBoolean(expression,event.getMessage());
    }

    @Override
    public boolean acceptsAll()
    {
        return expression == null;
    }

    protected abstract MuleEvent afterRouting(Exception exception, MuleEvent event);

    protected abstract MuleEvent beforeRouting(Exception exception, MuleEvent event);
}
