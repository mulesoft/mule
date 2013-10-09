/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.filter.Filter;

import java.util.List;

/**
 * <code>OutboundPassThroughRouter</code> allows outbound routing over a single
 * endpoint without any filtering. This class is used by Mule when a single outbound
 * router is set on a Service.
 */
public class OutboundPassThroughRouter extends FilteringOutboundRouter
{
    public OutboundPassThroughRouter()
    {
        super();
    }

    @Override
    public void addRoute(MessageProcessor target) throws MuleException
    {
        if (target == null)
        {
            return;
        }
        if (routes.size() == 1)
        {
            throw new IllegalArgumentException("Only one target can be set on the PassThrough router");
        }
        super.addRoute(target);
    }

    @Override
    public void setRoutes(List<MessageProcessor> endpoints) throws MuleException
    {
        if (endpoints.size() > 1)
        {
            throw new IllegalArgumentException("Only one endpoint can be set on the PassThrough router");
        }
        super.setRoutes(endpoints);
    }

    @Override
    public void setFilter(Filter filter)
    {
        throw new UnsupportedOperationException(
            "The Pass Through cannot use filters, use the FilteringOutboundRouter instead");
    }

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        if (routes == null || routes.size() == 0)
        {
            return event;
        }
        return super.route(event);
    }
}
