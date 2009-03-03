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

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

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

public class DefaultInboundRouterCollection extends AbstractRouterCollection implements InboundRouterCollection
{
    private final List endpoints = new CopyOnWriteArrayList();

    public DefaultInboundRouterCollection()
    {
        super(RouterStatistics.TYPE_INBOUND);
        //default for inbound routing
        setMatchAll(true);
    }

    public MuleMessage route(MuleEvent event) throws MessagingException
    {
        // If the endpoint has a logical name, use it, otherwise use the URI.
        String inboundEndpoint = 
            // Endpoint identifier (deprecated)
            event.getEndpoint().getEndpointURI().getEndpointName();

        if (StringUtils.isBlank(inboundEndpoint))
        {
            // Global endpoint
            inboundEndpoint = event.getEndpoint().getName();
        }
        if (StringUtils.isBlank(inboundEndpoint))
        {
            // URI
            inboundEndpoint = event.getEndpoint().getEndpointURI().getUri().toString();
        }
        event.getMessage().setProperty(MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY, inboundEndpoint);

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

        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
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
                        return getCatchAllStrategy().catchMessage(event.getMessage(), event.getSession());

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
                    MuleMessage messageResult = null;
                    /*
                        DON'T CHANGE THIS ITERATOR TYPE.
                        Looks like Iterator and Iterable for LinkedHashMap have different order.
                        This is critical to preserve the order for dispatching after e.g. resequencer.
                        MULE-4229
                     */
                    for (Iterator iterator = eventsToRoute.values().iterator(); iterator.hasNext();)
                    {
                        MuleEvent eventToRoute = (MuleEvent) iterator.next();

                        // Set the originating endpoint so we'll know where this event came from further down the pipeline.
                        if (event.getMessage().getProperty(MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY) == null)
                        {
                            event.getMessage().setProperty(MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY, inboundEndpoint);
                        }

                        if (event.isSynchronous())
                        {
                            messageResult = send(eventToRoute);
                        }
                        else
                        {
                            dispatch(eventToRoute);
                        }
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
        if(event.isSynchronous())
        {
            //This is required if the Router short-circuits the service and diverts processing elsewhere
            //The only example of this right now is the FowardingConsumer (<forwarding-router/>)
            return (lastEvent == null ? null : lastEvent.getMessage());
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

    public void addEndpoint(InboundEndpoint endpoint)
    {
        endpoints.add(endpoint);
    }

    public boolean removeEndpoint(InboundEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        if (endpoints != null)
        {
            this.endpoints.clear();
            // Ensure all endpoints are inbound endpoints
            // This will go when we start dropping support for 1.4 and start using 1.5
            for (Iterator it = endpoints.iterator(); it.hasNext();)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint) it.next();
                if (!(endpoint instanceof InboundEndpoint))
                {
                    throw new InvalidEndpointTypeException(CoreMessages.inboundRouterMustUseInboundEndpoints(this,
                        endpoint));
                }
            }
            this.endpoints.addAll(endpoints);
        }
        else
        {
            throw new IllegalArgumentException("List of endpoints = null");
        }
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see org.mule.api.routing.InboundRouterCollection
     */
    public InboundEndpoint getEndpoint(String name)
    {
        InboundEndpoint endpointDescriptor;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointDescriptor = (InboundEndpoint) iterator.next();
            if (endpointDescriptor.getName().equals(name))
            {
                return endpointDescriptor;
            }
        }
        return null;
    }
}
