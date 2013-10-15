/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.ExceptionHelper;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.ServiceStatistics;
import org.mule.util.CollectionUtils;

import java.util.List;

/**
 * <code>DefaultServiceExceptionStrategy</code> is the default exception handler
 * for components. The handler logs errors and will forward the message and exception
 * to an exception endpointUri if one is set on this Exception strategy
 */
public class DefaultServiceExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    /** Stop the flow/service when an exception occurs.  You will need to restart the flow/service manually after this (e.g, using JMX). */
    private boolean stopMessageProcessing;

    /** 
     * For IoC only 
     * @deprecated Use DefaultServiceExceptionStrategy(MuleContext muleContext) instead
     */
    public DefaultServiceExceptionStrategy()
    {
        super();
    }

    public DefaultServiceExceptionStrategy(MuleContext muleContext)
    {
        super();
        setMuleContext(muleContext);
    }

    @Override
    protected void doHandleException(Exception e, MuleEvent event)
    {
        FlowConstructStatistics statistics = getFlowConstructStatistics(event.getFlowConstruct());

        if (statistics != null && statistics.isEnabled())
        {
            statistics.incExecutionError();
        }

        super.doHandleException(DefaultMuleConfiguration.fullStackTraces ? e : (Exception) ExceptionHelper.sanitize(e), event);
    }

    @Override
    protected void logFatal(MuleEvent event, Throwable t)
    {
        FlowConstructStatistics statistics = getFlowConstructStatistics(event.getFlowConstruct());
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incFatalError();
        }

        super.logFatal(event, t);
    }

    @Override
    protected void routeException(MuleEvent event, MessageProcessor target, Throwable t)
    {
        super.routeException(event, target, t);
        List<MessageProcessor> processors = getMessageProcessors();
        if (CollectionUtils.isNotEmpty(processors) && getFlowConstructStatistics(event.getFlowConstruct()) instanceof ServiceStatistics)
        {
            ServiceStatistics statistics = getServiceStatistics(event.getFlowConstruct());
            if (statistics.isEnabled())
            {
                for (MessageProcessor endpoint : processors)
                {
                    statistics.getOutboundRouterStat().incrementRoutedMessage(endpoint);
                }
            }
        }

        if (stopMessageProcessing)
        {
            stopFlowConstruct();
        }
    }

    private void stopFlowConstruct()
    {
        if (flowConstruct instanceof Stoppable)
        {
            logger.info("Stopping flow '" + flowConstruct.getName() + "' due to exception");

            try
            {
                ((Lifecycle) flowConstruct).stop();
            }
            catch (MuleException e)
            {
                logger.error("Unable to stop flow '" + flowConstruct.getName() + "'", e);
            }
        }
        else
        {
            logger.warn("Flow is not stoppable");
        }
    }

    protected FlowConstructStatistics getFlowConstructStatistics(FlowConstruct flowConstruct)
    {
        if (flowConstruct != null )
        {
            return flowConstruct.getStatistics();
        }
        else
        {
            //this can happen, e.g. with event constructed to handle exceptions
            // logger.fatal("The Default Service Exception Strategy has been invoked but there is no current flow construct on the context. Please report this to dev@mule.codehaus.org");
            return null;
        }
    }

    protected ServiceStatistics getServiceStatistics(FlowConstruct flowConstruct)
    {
        FlowConstructStatistics stats = getFlowConstructStatistics(flowConstruct);
        if (!(stats instanceof ServiceStatistics))
        {
            //this should never happen, but JIC
            logger.fatal("The Default Service Exception Strategy has been invoked but there is no current service on the context. Please report this to dev@mule.codehaus.org");            
            return null;
        }
        return (ServiceStatistics) stats;
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
