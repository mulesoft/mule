/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.management.stats.RouterStatistics;

import java.util.List;

/**
 * <code>UMORouterCollection</code> defines the interface for a MessageRouter that
 * manages more than one router. A {@link UMORouterCatchAllStrategy} can be set on
 * this router to route unwanted or unfiltered events. If a catch strategy is not
 * set, the router just returns null.
 */

public interface UMORouterCollection
{
    void setRouters(List routers);

    List getRouters();

    void addRouter(UMORouter router);

    UMORouter removeRouter(UMORouter router);

    UMORouterCatchAllStrategy getCatchAllStrategy();

    void setCatchAllStrategy(UMORouterCatchAllStrategy catchAllStrategy);

    boolean isMatchAll();

    RouterStatistics getStatistics();

    void setStatistics(RouterStatistics stat);

    void setMatchAll(boolean matchAll);
}
