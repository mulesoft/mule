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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.SelectiveRouter;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.MessageFactory;
import org.mule.routing.outbound.DefaultRouterResultsHandler;

public abstract class AbstractSelectiveRouter extends AbstractRouter implements SelectiveRouter
{
    private final List<FilteredRoute> filteredRoutes = new ArrayList<FilteredRoute>();
    private MessageProcessor defaultProcessor;
    private final RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

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
        Collection<MessageProcessor> selectedProcessors = selectProcessors(event);

        if (!selectedProcessors.isEmpty())
        {
            return routeWithProcessors(selectedProcessors, event);
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
            event.getMessage(), null);
    }

    /**
     * @return the processors selected according to the specific router strategy or
     *         an empty collection (not null).
     */
    protected abstract Collection<MessageProcessor> selectProcessors(MuleEvent event);

    private MuleEvent routeWithProcessor(MessageProcessor processor, MuleEvent event) throws MuleException
    {
        return routeWithProcessors(Collections.singleton(processor), event);
    }

    private MuleEvent routeWithProcessors(Collection<MessageProcessor> processors, MuleEvent event)
        throws MuleException
    {
        List<MuleEvent> results = new ArrayList<MuleEvent>();

        for (MessageProcessor processor : processors)
        {
            results.add(processor.process(event));

            if (getRouterStatistics() != null)
            {
                getRouterStatistics().incrementRoutedMessage(event.getEndpoint());
            }
        }

        return resultsHandler.aggregateResults(results, event, muleContext);
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
