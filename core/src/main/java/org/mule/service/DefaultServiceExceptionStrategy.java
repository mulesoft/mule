/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.DefaultExceptionStrategy;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.management.stats.ServiceStatistics;
import org.mule.util.CollectionUtils;

import java.util.List;

/**
 * <code>DefaultServiceExceptionStrategy</code> is the default exception handler
 * for components. The handler logs errors and will forward the message and exception
 * to an exception endpointUri if one is set on this Exception strategy
 */
public class DefaultServiceExceptionStrategy extends DefaultExceptionStrategy
{
    public DefaultServiceExceptionStrategy()
    {
        super();
    }

    protected void defaultHandler(Throwable t)
    {
        ServiceStatistics statistics = getServiceStatistics();

        if (statistics != null)
        {
            statistics.incExecutionError();
        }

        super.defaultHandler(t);
    }

    protected void logFatal(MuleMessage message, Throwable t)
    {
        ServiceStatistics statistics = getServiceStatistics();
        if (statistics != null)
        {
            statistics.incFatalError();
        }

        super.logFatal(message, t);
    }

    protected void routeException(MuleMessage message, ImmutableEndpoint failedEndpoint, Throwable t)
    {
        super.routeException(message, failedEndpoint, t);
        List<ImmutableEndpoint> endpoints = getEndpoints(t);
        if (CollectionUtils.isNotEmpty(endpoints) && getServiceStatistics() != null)
        {
            ServiceStatistics statistics = getServiceStatistics();
            for (ImmutableEndpoint endpoint : endpoints)
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
        else if(event.getService()==null)
        {
            //this will ever happen, but JIC
            logger.fatal("The Default Service Exception Strategy has been invoked but there is no current service on the context. Please report this to dev@mule.codehaus.org");            
            return null;
        }
        else
        {
            return event.getService().getStatistics();
        }
    }
}
