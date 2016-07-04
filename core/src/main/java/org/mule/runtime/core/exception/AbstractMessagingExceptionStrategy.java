/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.DefaultExceptionPayload;

/**
 * Fire a notification, log exception, increment statistics, route the problematic message to a destination 
 * if one is configured (DLQ pattern), commit or rollback transaction if one exists, close any open streams.
 */
public abstract class AbstractMessagingExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandler
{
    /** 
     * Stop the flow/service when an exception occurs.  You will need to restart the flow/service manually after this (e.g, using JMX). 
     */
    private boolean stopMessageProcessing;

    public AbstractMessagingExceptionStrategy()
    {
    }

    public AbstractMessagingExceptionStrategy(MuleContext muleContext)
    {
        setMuleContext(muleContext);
    }

    @Override
    public MuleEvent handleException(Exception ex, MuleEvent event)
    {
        try
        {
            muleContext.getNotificationManager().fireNotification(new ExceptionStrategyNotification(event, ExceptionStrategyNotification.PROCESS_START));

            //keep legacy notifications
            fireNotification(ex);

            // Work with the root exception, not anything that wraps it
            //Throwable t = ExceptionHelper.getRootException(ex);

            logException(ex, event);
            doHandleException(ex, event);

            ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
            if (RequestContext.getEvent() != null)
            {
                RequestContext.setExceptionPayload(exceptionPayload);
            }
            event.setMessage(MuleMessage.builder(event.getMessage())
                                        .payload(NullPayload.getInstance())
                                        .exceptionPayload(exceptionPayload)
                                        .build());
            return event;
        }
        finally
        {
            muleContext.getNotificationManager().fireNotification(new ExceptionStrategyNotification(event, ExceptionStrategyNotification.PROCESS_END));
        }
    }

    protected void doHandleException(Exception ex, MuleEvent event)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incExecutionError();
        }

        // Left this here for backwards-compatibility, remove in the next major version.
        defaultHandler(ex);

        if (isRollback(ex))
        {
            logger.debug("Rolling back transaction");
            rollback(ex);

            logger.debug("Routing exception message");
            routeException(event, ex);
        }
        else
        {
            logger.debug("Routing exception message");
            routeException(event, ex);
        }

        closeStream(event.getMessage());

        if (stopMessageProcessing)
        {
            stopFlow(event.getFlowConstruct());
        }        
    }

    protected void stopFlow(FlowConstruct flow)
    {
        if (flow instanceof Stoppable)
        {
            logger.info("Stopping flow '" + flow.getName() + "' due to exception");

            try
            {
                ((Lifecycle) flow).stop();
            }
            catch (MuleException e)
            {
                logger.error("Unable to stop flow '" + flow.getName() + "'", e);
            }
        }
        else
        {
            logger.warn("Flow is not stoppable");
        }
    }

    public boolean isStopMessageProcessing()
    {
        return stopMessageProcessing;
    }

    public void setStopMessageProcessing(boolean stopMessageProcessing)
    {
        this.stopMessageProcessing = stopMessageProcessing;
    }

    /**
     * @deprecated Override doHandleException(Exception e, MuleEvent event) instead
     */
    // Left this here for backwards-compatibility, remove in the next major version.
    @Deprecated
    protected void defaultHandler(Throwable t)
    {
        // empty
    }
}
