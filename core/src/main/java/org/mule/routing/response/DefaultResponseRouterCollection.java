/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.ResponseRouter;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.routing.Router;
import org.mule.api.routing.RoutingException;
import org.mule.config.MuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;

import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

/**
 * <code>DefaultResponseRouterCollection</code> is a router that can be used to control how
 * the response in a request/response message flow is created. Main usecase is to
 * aggregate a set of asynchonous events into a single response
 */
public class DefaultResponseRouterCollection extends AbstractRouterCollection implements ResponseRouterCollection
{
    private volatile List endpoints = new CopyOnWriteArrayList();
    private volatile int timeout = MuleConfiguration.DEFAULT_TIMEOUT;
    private volatile boolean failOnTimeout = true;

    public DefaultResponseRouterCollection()
    {
        super(RouterStatistics.TYPE_RESPONSE);
    }


    public void initialise() throws InitialisationException
    {
        super.initialise();
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) iterator.next();
            endpoint.initialise();
        }
    }

    public void route(MuleEvent event) throws RoutingException
    {
        ResponseRouter router;
        for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
        {
            router = (ResponseRouter) iterator.next();
            router.process(event);
            // Update stats
            if (getStatistics().isEnabled())
            {
                getStatistics().incrementRoutedMessage(event.getEndpoint());
            }
        }
    }

    public MuleMessage getResponse(MuleMessage message) throws RoutingException
    {
        MuleMessage result = null;
        if (routers.size() == 0)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("There are no routers configured on the response router. Returning the current message");
            }
            result = message;
        }
        else
        {
            ResponseRouter router;
            for (Iterator iterator = getRouters().iterator(); iterator.hasNext();)
            {
                router = (ResponseRouter) iterator.next();
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
        // result = new DefaultMuleMessage(transformer.transform(result.getPayload()),
        // result.getProperties());
        // } catch (TransformerException e) {
        // throw new RoutingException(result, null);
        // }
        // }
        return result;

    }

    public void addRouter(Router router)
    {
        ((ResponseRouter) router).setTimeout(getTimeout());
        ((ResponseRouter) router).setFailOnTimeout(isFailOnTimeout());
        routers.add(router);
    }

    public ResponseRouter removeRouter(ResponseRouter router)
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

    public void addEndpoint(ImmutableEndpoint endpoint)
    {
        if (endpoint != null)
        {
            if (!endpoint.isInbound())
            {
                throw new InvalidEndpointTypeException(CoreMessages.responseRouterMustUseInboundEndpoints(
                    this, endpoint));
            }
            endpoints.add(endpoint);
        }
        else
        {
            throw new IllegalArgumentException("endpoint = null");
        }
    }

    public boolean removeEndpoint(ImmutableEndpoint endpoint)
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

            // Ensure all endpoints are response endpoints
            for (Iterator it = this.endpoints.iterator(); it.hasNext();)
            {
                ImmutableEndpoint endpoint=(ImmutableEndpoint) it.next();
                if (!endpoint.isInbound())
                {
                    throw new InvalidEndpointTypeException(CoreMessages.responseRouterMustUseInboundEndpoints(
                        this, endpoint));
                }
            }

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
    public ImmutableEndpoint getEndpoint(String name)
    {
        ImmutableEndpoint endpointDescriptor;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointDescriptor = (ImmutableEndpoint) iterator.next();
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


    public boolean isFailOnTimeout()
    {
        return failOnTimeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }


    public boolean hasEndpoints()
    {
        return !getEndpoints().isEmpty();
    }
}
