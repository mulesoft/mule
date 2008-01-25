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
import org.mule.api.service.Service;
import org.mule.lifecycle.DefaultLifecycleAdapter;
import org.mule.management.stats.ServiceStatistics;

/**
 * <code>DefaultServiceExceptionStrategy</code> is the default exception handler
 * for components. The handler logs errors and will forward the message and exception
 * to an exception endpointUri if one is set on this Exception strategy
 */
public class DefaultServiceExceptionStrategy extends DefaultExceptionStrategy
{
    /**
     * The service to which the Exception handler belongs
     */
    protected Service service;

    protected ServiceStatistics statistics;

    public DefaultServiceExceptionStrategy()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param service the owner of this exception strategy
     * @see DefaultLifecycleAdapter
     */
    public DefaultServiceExceptionStrategy(Service service)
    {
        super();
        setService(service);
    }

    /**
     * @return the UniversalMessageObject to which this handler is attached
     */
    public Service getService()
    {
        return service;
    }

    protected void defaultHandler(Throwable t)
    {
        // Lazy initialisation of the service
        // This strategy should be associated with only one service
        // and thus there is no concurrency problem
        if (service == null)
        {
            MuleEvent event = RequestContext.getEvent();
            if (event == null)
            {
                // very bad should not happen
                logger.fatal("The Default Service Exception Strategy has been invoked but there is no current event on the context");
                logger.fatal("The error is: " + t.getMessage(), t);
            }
            else
            {
                setService(event.getService());
            }
        }

        if (statistics != null)
        {
            statistics.incExecutionError();
        }
        
        super.defaultHandler(t);
    }

    protected void logFatal(MuleMessage message, Throwable t)
    {
        super.logFatal(message, t);
        if (statistics != null)
        {
            statistics.incFatalError();
        }
    }

    protected void routeException(MuleMessage message, ImmutableEndpoint failedEndpoint, Throwable t)
    {
        ImmutableEndpoint ep = getEndpoint(t);
        if (ep != null)
        {
            super.routeException(message, failedEndpoint, t);
            if (statistics != null)
            {
                statistics.getOutboundRouterStat().incrementRoutedMessage(ep);
            }
        }
    }

    public void setService(Service service)
    {
        this.service = service;
        if (service instanceof AbstractService)
        {
            if (statistics != null)
            {
                this.statistics = ((AbstractService) service).getStatistics();
            }
        }
    }
}
