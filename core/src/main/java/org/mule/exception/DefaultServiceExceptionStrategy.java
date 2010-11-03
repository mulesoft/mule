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
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.ExceptionHelper;
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
    protected void defaultHandler(Throwable t)
    {
        ServiceStatistics statistics = getServiceStatistics();

        if (statistics != null)
        {
            statistics.incExecutionError();
        }

        super.defaultHandler(DefaultMuleConfiguration.fullStackTraces ? t : ExceptionHelper.sanitize(t));
    }

    @Override
    protected void logFatal(MuleMessage message, Throwable t)
    {
        ServiceStatistics statistics = getServiceStatistics();
        if (statistics != null)
        {
            statistics.incFatalError();
        }

        super.logFatal(message, t);
    }

    @Override
    protected void routeException(MuleMessage message, MessageProcessor target, Throwable t)
    {
        super.routeException(message, target, t);
        List<MessageProcessor> processors = getMessageProcessors(t);
        if (CollectionUtils.isNotEmpty(processors) && getServiceStatistics() != null)
        {
            ServiceStatistics statistics = getServiceStatistics();
            for (MessageProcessor endpoint : processors)
            {
                statistics.getOutboundRouterStat().incrementRoutedMessage(endpoint);
            }
        }
    }

    protected ServiceStatistics getServiceStatistics()
    {
        MuleEvent event = RequestContext.getEvent();
        if (event == null)
        {
            // very bad should not happen
            logger.fatal("The Default Service Exception Strategy has been invoked but there is no current event on the context");
            //logger.fatal("The error is: " + t.getMessage(), t);
            return null;
        }
        else if(event.getFlowConstruct()!=null && event.getFlowConstruct() instanceof Service)
        {
            return ((Service) event.getFlowConstruct()).getStatistics();
        }
        else
        {
            //this will ever happen, but JIC
            logger.fatal("The Default Service Exception Strategy has been invoked but there is no current service on the context. Please report this to dev@mule.codehaus.org");            
            return null;
        }
    }
}
