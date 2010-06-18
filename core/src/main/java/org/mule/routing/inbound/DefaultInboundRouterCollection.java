/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.RoutingException;
import org.mule.api.source.CompositeMessageSource;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.source.StartablePatternAwareCompositeMessageSource;
import org.mule.util.StringMessageUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

/**
 * <code>DefaultInboundRouterCollection</code> is a collection of routers that will be
 * invoked when an event is received. It is responsible for managing a collection of
 * routers and also executing the routing logic. Each router must match against the
 * current event for the event to be routed.
 */

public class DefaultInboundRouterCollection extends AbstractRouterCollection
    implements InboundRouterCollection
{
    
    protected CompositeMessageSource sourceAggregator = new StartablePatternAwareCompositeMessageSource();
    
    @SuppressWarnings("unchecked")
    private final List<InboundEndpoint> endpoints = new CopyOnWriteArrayList();

    private MessageProcessor listener;

    public DefaultInboundRouterCollection()
    {
        super(RouterStatistics.TYPE_INBOUND);
        //default for inbound routing
        setMatchAll(true);
    }

    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        if (endpoints.size() > 0 && routers.size() == 0)
        {
            addRouter(new InboundPassThroughRouter());
        }

        String componentName = event.getSession().getService().getName();

        /*
            Event Map must be ordered: MULE-4229
         */
        Map<String, MuleEvent> eventsToRoute = new LinkedHashMap<String, MuleEvent>(2);
        boolean noRoute = true;
        boolean match = false;
        InboundRouter inboundRouter;
        MuleEvent lastEvent= null;

        for (Iterator<?> iterator = getRouters().iterator(); iterator.hasNext();)
        {
            inboundRouter = (InboundRouter) iterator.next();

            if (inboundRouter.isMatch(event))
            {
                match = true;
                MuleEvent[] events = inboundRouter.process(event);
                if(events!=null)
                {
                    for (MuleEvent event1 : events)
                    {
                        lastEvent = event1;
                        eventsToRoute.put(lastEvent.getId(), lastEvent);
                    }
                }

                noRoute = (events == null);
                if (!isMatchAll())
                {
                    break;
                }
            }
        }

        // If the stopFurtherProcessing flag has been set
        // do not route events to the service.
        // This is the case when using a ForwardingConsumer
        // inbound router for example.
        if (!event.isStopFurtherProcessing())
        {
            if (noRoute)
            {
                // Update stats
                if (getStatistics().isEnabled())
                {
                    getStatistics().incrementNoRoutedMessage();
                }
                if (!match)
                {
                    if (getCatchAllStrategy() != null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Message did not match any routers on: " + componentName
                                         + " - invoking catch all strategy");
                        }
                        if (getStatistics().isEnabled())
                        {
                            getStatistics().incrementCaughtMessage();
                        }
                        MuleMessage result = getCatchAllStrategy().catchMessage(event.getMessage(),
                            event.getSession());
                        if (result != null)
                        {
                            return new DefaultMuleEvent(result, event);
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else
                    {
                        logger.warn("Message did not match any routers on: "
                                    + componentName
                                    + " and there is no catch all strategy configured on this router.  Disposing message: " + event);
                        if (logger.isDebugEnabled())
                        {
                            try
                            {
                                logger.warn("Message fragment is: "
                                            + StringMessageUtils.truncate(event.getMessageAsString(), 100,
                                                true));
                            }
                            catch (MuleException e)
                            {
                                // ignore
                            }
                        }
                    }
                }
            }
            else
            {
                try
                {
                    MuleEvent messageResult = null;
                    /*
                        DON'T CHANGE THIS ITERATOR TYPE.
                        Looks like Iterator and Iterable for LinkedHashMap have different order.
                        This is critical to preserve the order for dispatching after e.g. resequencer.
                        MULE-4229
                     */
                    for (Iterator iterator = eventsToRoute.values().iterator(); iterator.hasNext();)
                    {
                        MuleEvent eventToRoute = (MuleEvent) iterator.next();
                        messageResult = listener.process(eventToRoute);
                        // Update stats
                        if (getStatistics().isEnabled())
                        {
                            getStatistics().incrementRoutedMessage(eventToRoute.getEndpoint());
                        }
                    }
                    return messageResult;
                }
                catch (MuleException e)
                {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
        }
        if (event.isSynchronous())
        {
            // This is required if the Router short-circuits the service and diverts
            // processing elsewhere
            // The only example of this right now is the FowardingConsumer
            // (<forwarding-router/>)
            return lastEvent;
        }
        else
        {
            return null;
        }

    }

    public void dispatch(MuleEvent event) throws MuleException
    {
        event.getSession().dispatchEvent(event);
    }

    public MuleMessage send(MuleEvent event) throws MuleException
    {
        return event.getSession().sendEvent(event);
    }

    public void addRouter(InboundRouter router)
    {
        routers.add(router);
    }

    public InboundRouter removeRouter(InboundRouter router)
    {
        if (routers.remove(router))
        {
            return router;
        }
        else
        {
            return null;
        }
    }

    public void addEndpoint(InboundEndpoint endpoint) throws MuleException
    {
        endpoints.add(endpoint);
        sourceAggregator.addSource(endpoint);
    }

    public boolean removeEndpoint(InboundEndpoint endpoint) throws MuleException
    {
        sourceAggregator.removeSource(endpoint);
        return endpoints.remove(endpoint);
    }

    public List<InboundEndpoint> getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List<InboundEndpoint> endpointList) throws MuleException
    {
        if (endpoints != null)
        {
            for (InboundEndpoint endpoint : endpoints)
            {
                removeEndpoint(endpoint);
            }
            this.endpoints.clear();
            for (InboundEndpoint endpoint : endpointList)
            {
                addEndpoint(endpoint);
            }
        }
        else
        {
            throw new IllegalArgumentException("List of endpoints = null");
        }
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or <code>null</code> if the endpointUri is not registered
     * @see org.mule.api.routing.InboundRouterCollection
     */
    public InboundEndpoint getEndpoint(String name)
    {
        for (InboundEndpoint endpoint : endpoints)
        {
            if (endpoint.getName().equals(name))
            {
                return endpoint;
            }
        }
        
        return null;
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public CompositeMessageSource getSourceAggregator()
    {
        return sourceAggregator;
    }    
}
