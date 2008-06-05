/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.Router;
import org.mule.management.stats.RouterStatistics;

/**
 * Implements the shared methods that all routers have. The implementations of the different
 * router types can vary depending on their usage pattern. The types of router are inbound, outbound
 * response and nested.
 */
public abstract class AbstractRouter implements Router, MuleContextAware
{

    private RouterStatistics routerStatistics;

    protected MuleContext muleContext;

    public void initialise() throws InitialisationException
    {
        // default impl does nothing
    }

    public void dispose()
    {
        // Template
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setRouterStatistics(RouterStatistics stats)
    {
        this.routerStatistics = stats;
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }
}
