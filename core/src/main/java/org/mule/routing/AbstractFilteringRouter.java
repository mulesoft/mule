/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.FilteringRouter;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.MessageFactory;

public abstract class AbstractFilteringRouter extends AbstractRouter implements FilteringRouter
{
    private final List<FilteredRoute> filteredRoutes = new ArrayList<FilteredRoute>();
    private MessageProcessor defaultProcessor;

    public void addRoute(MessageProcessor processor, Filter filter)
    {
        synchronized (filteredRoutes)
        {
            filteredRoutes.add(new FilteredRoute(processor, filter));
        }
    }

    public void removeRoute(MessageProcessor processor)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                filteredRoutes.remove(index);
            }
        });
    }

    public void updateRoute(final MessageProcessor processor, final Filter filter)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                filteredRoutes.set(index, new FilteredRoute(processor, filter));
            }
        });
    }

    public void setDefaultRoute(MessageProcessor processor)
    {
        defaultProcessor = processor;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MessageProcessor selectedProcessor = selectProcessor(event);

        if (selectedProcessor != null)
        {
            return routeWithProcessor(selectedProcessor, event);
        }

        if (defaultProcessor != null)
        {
            return routeWithProcessor(defaultProcessor, event);
        }

        if (getRouterStatistics() != null)
        {
            getRouterStatistics().incrementNoRoutedMessage();
        }

        throw new RoutePathNotFoundException(
            MessageFactory.createStaticMessage("Can't process message because no route has been found matching any filter and no default route is defined"),
            event.getMessage(), this);
    }

    /**
     * @return the processor selected according to the specific router strategy or
     *         null.
     */
    protected abstract MessageProcessor selectProcessor(MuleEvent event);

    private MuleEvent routeWithProcessor(MessageProcessor processor, MuleEvent event) throws MuleException
    {
        MuleEvent result = processor.process(event);

        if (getRouterStatistics() != null)
        {
            getRouterStatistics().incrementRoutedMessage(event.getEndpoint());
        }

        return result;
    }

    protected List<FilteredRoute> getFilteredRoutes()
    {
        return Collections.unmodifiableList(filteredRoutes);
    }

    private interface RoutesUpdater
    {
        void updateAt(int index);
    }

    private void updateRoute(MessageProcessor processor, RoutesUpdater routesUpdater)
    {
        synchronized (filteredRoutes)
        {

            for (int i = 0; i < filteredRoutes.size(); i++)
            {
                if (filteredRoutes.get(i).processor.equals(processor))
                {
                    routesUpdater.updateAt(i);
                }
            }
        }
    }

    protected static class FilteredRoute
    {
        final MessageProcessor processor;
        final Filter filter;

        public FilteredRoute(MessageProcessor processor, Filter filter)
        {
            this.processor = processor;
            this.filter = filter;
        }
    }

}
