/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.util.StringMessageUtils;

import java.util.Iterator;
import java.util.List;

/**
 * <code>InboundMessageRouter</code> is a collection of routers that will be
 * invoked when an event is received It is responsible for manageing a collection of
 * routers and also executing the routing logic. Each router must match against the
 * current event for the event to be routed.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason </a>
 * @version $Revision$
 */

public class InboundMessageRouter extends AbstractRouterCollection implements UMOInboundMessageRouter
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(InboundMessageRouter.class);

    private List endpoints = new CopyOnWriteArrayList();

    public InboundMessageRouter()
    {
        super(RouterStatistics.TYPE_INBOUND);
    }

    public UMOMessage route(UMOEvent event) throws MessagingException
    {
        if (endpoints.size() > 0 && routers.size() == 0)
        {
            addRouter(new InboundPassThroughRouter());
        }

        String componentName = event.getSession().getComponent().getDescriptor().getName();

        UMOEvent[] eventsToRoute = null;
        boolean noRoute = true;
        boolean match = false;
        UMOInboundRouter umoInboundRouter = null;

        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
        {
            umoInboundRouter = (UMOInboundRouter)iterator.next();

            if (umoInboundRouter.isMatch(event))
            {
                match = true;
                eventsToRoute = umoInboundRouter.process(event);
                noRoute = (eventsToRoute == null);
                if (!matchAll)
                {
                    break;
                }
            }
        }

        // If the stopFurtherProcessing flag has been set
        // do not route events to the component.
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
                        return getCatchAllStrategy().catchMessage(event.getMessage(), event.getSession(),
                            event.isSynchronous());

                    }
                    else
                    {
                        logger.warn("Message did not match any routers on: "
                                    + componentName
                                    + " and there is no catch all strategy configured on this router.  Disposing message.");
                        if (logger.isDebugEnabled())
                        {
                            try
                            {
                                logger.warn("Message fragment is: "
                                            + StringMessageUtils.truncate(event.getMessageAsString(), 100,
                                                true));
                            }
                            catch (UMOException e)
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
                    UMOMessage messageResult = null;
                    if (eventsToRoute != null)
                    {
                        for (int i = 0; i < eventsToRoute.length; i++)
                        {
                            if (event.isSynchronous())
                            {
                                messageResult = send(eventsToRoute[i]);
                            }
                            else
                            {
                                dispatch(eventsToRoute[i]);
                            }
                            // Update stats
                            if (getStatistics().isEnabled())
                            {
                                getStatistics().incrementRoutedMessage(eventsToRoute[i].getEndpoint());
                            }
                        }
                    }
                    return messageResult;
                }
                catch (UMOException e)
                {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
        }
        return (eventsToRoute != null && eventsToRoute.length > 0
                        ? eventsToRoute[eventsToRoute.length - 1].getMessage() : null);

    }

    public void dispatch(UMOEvent event) throws UMOException
    {
        event.getSession().dispatchEvent(event);
    }

    public UMOMessage send(UMOEvent event) throws UMOException
    {

        return event.getSession().sendEvent(event);
    }

    public void addRouter(UMOInboundRouter router)
    {
        routers.add(router);
    }

    public UMOInboundRouter removeRouter(UMOInboundRouter router)
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

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint != null)
        {
            endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
            endpoints.add(endpoint);
        }
        else
        {
            throw new NullPointerException("endpoint = null");
        }
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
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
            this.endpoints.addAll(endpoints);

            // Force all endpoints' type to RECEIVER just in case.
            for (Iterator it = this.endpoints.iterator(); it.hasNext();)
            {
                ((UMOEndpoint)it.next()).setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
            }
        }
        else
        {
            throw new NullPointerException("List of endpoints = null");
        }
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     * @see org.mule.umo.routing.UMOInboundMessageRouter
     */
    public UMOEndpoint getEndpoint(String name)
    {
        UMOEndpoint endpointDescriptor;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointDescriptor = (UMOEndpoint)iterator.next();
            if (endpointDescriptor.getName().equals(name))
            {
                return endpointDescriptor;
            }
        }
        return null;
    }
}
