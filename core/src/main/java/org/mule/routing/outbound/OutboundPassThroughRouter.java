/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
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
    public void addTarget(MessageProcessor target)
    {
        if (target == null)
        {
            return;
        }
        if (targets.size() == 1)
        {
            throw new IllegalArgumentException("Only one target can be set on the PassThrough router");
        }
        super.addTarget(target);
    }

    @Override
    public void setTargets(List<MessageProcessor> endpoints)
    {
        if (endpoints.size() > 1)
        {
            throw new IllegalArgumentException("Only one endpoint can be set on the PassThrough router");
        }
        super.setTargets(endpoints);
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
        if (targets == null || targets.size() == 0)
        {
            return event;
        }
        return super.route(event);
    }
}
