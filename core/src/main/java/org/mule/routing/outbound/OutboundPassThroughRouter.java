/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
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
