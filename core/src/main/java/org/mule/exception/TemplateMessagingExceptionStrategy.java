/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static java.lang.Boolean.getBoolean;
import static org.mule.api.config.MuleProperties.DISABLE_ERROR_COUNT_ON_ERROR_NOTIFICATION_DISABLED;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.requestreply.ReplyToPropertyRequestReplyReplier;
import org.mule.transaction.TransactionCoordination;

public abstract class TemplateMessagingExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor
{

    private MessageProcessorChain configuredMessageProcessors;
    private MessageProcessor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();
    private String when;
    private boolean handleException;
    private boolean disableErrorCountOnErrorNotificationsDisabled = getBoolean(DISABLE_ERROR_COUNT_ON_ERROR_NOTIFICATION_DISABLED);

    final public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        try
        {
            boolean nonBlocking = event.isAllowNonBlocking() && event.getReplyToHandler() instanceof NonBlockingReplyToHandler;

            muleContext.getNotificationManager().fireNotification(new ExceptionStrategyNotification(event, ExceptionStrategyNotification.PROCESS_START));
            FlowConstruct flowConstruct = event.getFlowConstruct();
            fireNotification(exception);
            logException(exception, event);
            processStatistics(event);
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));

            // MULE-8551 Still need to add support for non-blocking components in excepton strategies.
            if(nonBlocking)
            {
                // Make event synchronous and clear replyToHandler.
                event = new DefaultMuleEvent(event, event.getFlowConstruct(), null, null, true);
            }
            event = beforeRouting(exception, event);
            event = route(event, exception);
            processOutboundRouterStatistics(flowConstruct);
            event = afterRouting(exception, event);
            markExceptionAsHandledIfRequired(exception);
            if (event != null && !VoidMuleEvent.getInstance().equals(event))
            {
                // Only process reply-to if non-blocking is not enabled. Checking the exchange pattern is not sufficient
                // because JMS inbound endpoints for example use a REQUEST_RESPONSE exchange pattern and async processing.
                if (!nonBlocking)
                {
                    processReplyTo(event, exception);
                }
                closeStream(event.getMessage());
                nullifyExceptionPayloadIfRequired(event);
            }
            return event;
        }
        catch (Exception e)
        {
            MessagingException messagingException;
            if (e instanceof MessagingException)
            {
                messagingException = (MessagingException) e;
            }
            else 
            {
                messagingException = new MessagingException(event, e); 
            }
            try
            {
                logger.error("Exception during exception strategy execution");
                doLogException(e);
                TransactionCoordination.getInstance().rollbackCurrentTransaction();
            }
            catch (Exception ex)
            {
                //Do nothing
            }
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(messagingException));
            return event;
        }
        finally
        {
            muleContext.getNotificationManager().fireNotification(new ExceptionStrategyNotification(event, ExceptionStrategyNotification.PROCESS_END));
        }
    }

    private void markExceptionAsHandledIfRequired(Exception exception)
    {
        if (handleException)
        {
            markExceptionAsHandled(exception);
        }
    }
    
    protected void markExceptionAsHandled(Exception exception)
    {
        if (exception instanceof MessagingException)
        {
            ((MessagingException)exception).setHandled(true);
        }
    }

    protected void processReplyTo(MuleEvent event, Exception e)
    {
        try
        {
            replyToMessageProcessor.process(event);
        }
        catch (MuleException ex)
        {
            logFatal(event,ex);
        }
    }

    protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
    {
        if (this.handleException)
        {
            event.getMessage().setExceptionPayload(null);
        }
    }

    private void processStatistics(MuleEvent event)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled() && mustCountErrorInStatistics())
        {
            statistics.incExecutionError();
        }
    }

    private boolean mustCountErrorInStatistics()
    {
        return enableNotifications || !disableErrorCountOnErrorNotificationsDisabled;
    }

    protected MuleEvent route(MuleEvent event, Exception t)
    {
        if (!getMessageProcessors().isEmpty())
        {
            try
            {
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(t));
                MuleEvent result = configuredMessageProcessors.process(event);                
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


    public void setWhen(String when)
    {
        this.when = when;
    }

    public boolean accept(MuleEvent event)
    {
        return acceptsAll() || acceptsEvent(event) || muleContext.getExpressionManager().evaluateBoolean(when, event);
    }

    /**
     * Determines if the exception strategy should process or not a message inside a choice exception strategy.
     *
     * Useful for exception strategies which ALWAYS must accept certain types of events despite when condition is not true.
     *
     * @param event   The MuleEvent being processed
     * @return  true if it should process the exception for the current event, false otherwise.
     */
    protected boolean acceptsEvent(MuleEvent event)
    {
        return false;
    }

    @Override
    public boolean acceptsAll()
    {
        return when == null;
    }

    protected MuleEvent afterRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
    {
        return event;
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        return;
    }

    public void setHandleException(boolean handleException)
    {
        this.handleException = handleException;
    }
}
