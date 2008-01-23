/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.DefaultExceptionStrategy;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.lifecycle.DefaultLifecycleAdapter;
import org.mule.management.stats.ComponentStatistics;

/**
 * <code>DefaultComponentExceptionStrategy</code> is the default exception handler
 * for components. The handler logs errors and will forward the message and exception
 * to an exception endpointUri if one is set on this Exception strategy
 */
public class DefaultComponentExceptionStrategy extends DefaultExceptionStrategy
{
    /**
     * The component to which the Exception handler belongs
     */
    protected Component component;

    protected ComponentStatistics statistics;

    public DefaultComponentExceptionStrategy()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param component the owner of this exception strategy
     * @see DefaultLifecycleAdapter
     */
    public DefaultComponentExceptionStrategy(Component component)
    {
        super();
        setComponent(component);
    }

    /**
     * @return the UniversalMessageObject to which this handler is attached
     */
    public Component getComponent()
    {
        return component;
    }

    protected void defaultHandler(Throwable t)
    {
        // Lazy initialisation of the component
        // This strategy should be associated with only one component
        // and thus there is no concurrency problem
        if (component == null)
        {
            MuleEvent event = RequestContext.getEvent();
            if (event == null)
            {
                // very bad should not happen
                logger.fatal("The Default Component Exception Strategy has been invoked but there is no current event on the context");
                logger.fatal("The error is: " + t.getMessage(), t);
            }
            else
            {
                setComponent(event.getComponent());
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

    public void setComponent(Component component)
    {
        this.component = component;
        if (component instanceof AbstractComponent)
        {
            if (statistics != null)
            {
                this.statistics = ((AbstractComponent) component).getStatistics();
            }
        }
    }
}
