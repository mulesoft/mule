/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.umo.routing.UMORouter;
import org.mule.management.stats.RouterStatistics;

/**
 * Implements the shared methods that all routers have. The implementations of the different
 * router types can vary depending on their usage pattern. The types of router are inbound, outbound
 * response and nested.
 */
public abstract class AbstractRouter implements UMORouter
{
    private RouterStatistics routerStatistics;


    public void setRouterStatistics(RouterStatistics stats)
    {
        this.routerStatistics = stats;
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }
}
