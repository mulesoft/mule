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

import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.ResponseRouter;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.routing.Router;
import org.mule.api.routing.RoutingException;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.inbound.DefaultInboundRouterCollection;

import java.util.Iterator;

/**
 * <code>DefaultResponseRouterCollection</code> is a router that can be used to
 * control how the response in a request/response message flow is created. Main
 * usecase is to aggregate a set of asynchonous events into a single response
 */
public class DefaultResponseRouterCollection extends DefaultInboundRouterCollection
    implements ResponseRouterCollection
{
    private volatile int timeout = -1; // undefined
    private volatile boolean failOnTimeout = true;

    public DefaultResponseRouterCollection()
    {
        statistics = new RouterStatistics(RouterStatistics.TYPE_RESPONSE);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (timeout == -1) // undefined
        {
            setTimeout(muleContext.getConfiguration().getDefaultResponseTimeout());
        }
        super.initialise();
    }

    public MuleEvent process(MuleEvent event) throws MessagingException
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
        return null;
    }

    public MuleEvent getResponse(MuleEvent message) throws RoutingException
    {
        MuleEvent result = null;
        if (routers.size() == 0)
        {
            if (logger.isDebugEnabled())
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

            // Copy event because the async-reply message was received by a different
            // receiver thread (or the senders dispatcher thread in case of vm
            // with queueEvents="false") and the current thread may need to mutate
            // the even. See MULE-4370
            return OptimizedRequestContext.criticalSetEvent(result);
        }

        return result;

    }

    @Override
    public void addRouter(Router router)
    {
        ((ResponseRouter) router).setTimeout(getTimeout());
        ((ResponseRouter) router).setFailOnTimeout(isFailOnTimeout());
        routers.add(router);
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
