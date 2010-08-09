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

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.api.routing.RouterStatisticsRecorder;
import org.mule.api.routing.SelectiveRouter;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.RouterStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class AbstractSelectiveRouter implements SelectiveRouter, RouterStatisticsRecorder
{
    private final List<ConditionalMessageProcessor> conditionalMessageProcessors = new ArrayList<ConditionalMessageProcessor>();
    private final RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    private MessageProcessor defaultProcessor;
    private RouterStatistics routerStatistics;

    public AbstractSelectiveRouter()
    {
        routerStatistics = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
    }

    public void addRoute(MessageProcessor processor, Filter filter)
    {
        addRoute(new ConditionalMessageProcessor(processor, filter));
    }

    public void addRoute(ConditionalMessageProcessor cmp)
    {
        synchronized (conditionalMessageProcessors)
        {
            conditionalMessageProcessors.add(cmp);
        }
    }

    public void removeRoute(MessageProcessor processor)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                conditionalMessageProcessors.remove(index);
            }
        });
    }

    public void updateRoute(final MessageProcessor processor, final Filter filter)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                conditionalMessageProcessors.set(index, new ConditionalMessageProcessor(processor, filter));
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
            event, this);
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
            processEventWithProcessor(event, processor, results);
        }

        return resultsHandler.aggregateResults(results, event, event.getMuleContext());
    }

    private void processEventWithProcessor(MuleEvent event,
                                           MessageProcessor processor,
                                           List<MuleEvent> results) throws MuleException
    {
        MuleEvent processedEvent = event;

        if (processor instanceof OutboundEndpoint)
        {
            processedEvent = new DefaultMuleEvent(event.getMessage(), (OutboundEndpoint) processor,
                event.getSession());
        }

        results.add(processor.process(processedEvent));

        if (getRouterStatistics() != null)
        {
            getRouterStatistics().incrementRoutedMessage(event.getEndpoint());
        }
    }

    protected List<ConditionalMessageProcessor> getConditionalMessageProcessors()
    {
        return conditionalMessageProcessors;
    }

    private interface RoutesUpdater
    {
        void updateAt(int index);
    }

    private void updateRoute(MessageProcessor processor, RoutesUpdater routesUpdater)
    {
        synchronized (conditionalMessageProcessors)
        {

            for (int i = 0; i < conditionalMessageProcessors.size(); i++)
            {
                if (conditionalMessageProcessors.get(i).getMessageProcessor().equals(processor))
                {
                    routesUpdater.updateAt(i);
                }
            }
        }
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }
    
    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
