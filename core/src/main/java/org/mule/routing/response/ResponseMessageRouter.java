/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.config.MuleConfiguration;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.routing.UMOResponseRouter;
import org.mule.umo.routing.UMORouter;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.util.Iterator;
import java.util.List;

/**
 * <code>ResponseMessageRouter</code> is a router that can be used to control how
 * the response in a request/response message flow is created. Main usecase is to
 * aggregate a set of asynchonous events into a single response
 */
public class ResponseMessageRouter extends AbstractRouterCollection implements UMOResponseMessageRouter
{
    private volatile List endpoints = new CopyOnWriteArrayList();
    private volatile int timeout = MuleConfiguration.DEFAULT_TIMEOUT;

    public ResponseMessageRouter()
    {
        super(RouterStatistics.TYPE_RESPONSE);
    }

    public void route(UMOEvent event) throws RoutingException
    {
        UMOResponseRouter router = null;
        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
        {
            router = (UMOResponseRouter)iterator.next();
            router.process(event);
            // Update stats
            if (getStatistics().isEnabled())
            {
                getStatistics().incrementRoutedMessage(event.getEndpoint());
            }
        }
    }

    public UMOMessage getResponse(UMOMessage message) throws RoutingException
    {
        UMOMessage result = null;
        if (routers.size() == 0)
        {
            logger.warn("There are no routers configured on the response router. Returning the current message");
            result = message;
        }
        else
        {
            UMOResponseRouter router = null;
            for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
            {
                router = (UMOResponseRouter)iterator.next();
                result = router.getResponse(message);
            }

            if (result == null)
            {
                // Update stats
                if (getStatistics().isEnabled())
                {
                    getStatistics().incrementNoRoutedMessage();
                }
            }
        }

        // if (result != null && transformer != null) {
        // try {
        // result = new MuleMessage(transformer.transform(result.getPayload()),
        // result.getProperties());
        // } catch (TransformerException e) {
        // throw new RoutingException(result, null);
        // }
        // }
        return result;

    }

    public void addRouter(UMORouter router)
    {
        ((UMOResponseRouter)router).setTimeout(getTimeout());
        routers.add(router);
    }

    public UMOResponseRouter removeRouter(UMOResponseRouter router)
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
            endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RESPONSE);
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

            // Force all endpoints' type to RESPONSE just in case.
            for (Iterator it = this.endpoints.iterator(); it.hasNext();)
            {
                ((UMOEndpoint)it.next()).setType(UMOEndpoint.ENDPOINT_TYPE_RESPONSE);
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

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

}
