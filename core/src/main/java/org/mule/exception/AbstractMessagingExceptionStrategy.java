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

import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Stoppable;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;

/**
 * Fire a notification, log exception, increment statistics, route the problematic message to a destination 
 * if one is configured (DLQ pattern), commit or rollback transaction if one exists, close any open streams.
 */
public abstract class AbstractMessagingExceptionStrategy extends AbstractExceptionStrategy implements MessagingExceptionHandler
{
    /** 
     * Stop the flow/service when an exception occurs.  You will need to restart the flow/service manually after this (e.g, using JMX). 
     */
    private boolean stopMessageProcessing;

    public AbstractMessagingExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
    }

    public MuleEvent handleException(Exception ex, MuleEvent event)
    {
        fireNotification(ex);

        // Work with the root exception, not anything that wraps it
        //Throwable t = ExceptionHelper.getRootException(ex);

        logException(ex);
        
        doHandleException(ex, event);

        ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(exceptionPayload);
        }
        event.getMessage().setPayload(NullPayload.getInstance());
        event.getMessage().setExceptionPayload(exceptionPayload);
        return event;
    }
    
    protected void doHandleException(Exception ex, MuleEvent event)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incExecutionError();
        }

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
}
