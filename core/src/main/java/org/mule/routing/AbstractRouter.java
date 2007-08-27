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

import org.mule.impl.ManagementContextAware;
import org.mule.management.stats.RouterStatistics;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.routing.UMORouter;

/**
 * Implements the shared methods that all routers have. The implementations of the different
 * router types can vary depending on their usage pattern. The types of router are inbound, outbound
 * response and nested.
 */
public abstract class AbstractRouter implements UMORouter, ManagementContextAware
{
    private RouterStatistics routerStatistics;

    private UMOManagementContext managementContext;


    public void initialise() throws InitialisationException
    {
        //template
    }


    public void dispose()
    {
        // Template
    }

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public UMOManagementContext getManagementContext()
    {
        return managementContext;
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
