/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.mule.management.stats.RouterStatistics;
import org.mule.umo.routing.UMORouter;
import org.mule.umo.routing.UMORouterCatchAllStrategy;
import org.mule.umo.routing.UMORouterCollection;

import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractRouterCollection</code> provides common method implementations of
 * router collections for in and outbound routers.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason </a>
 * @version $Revision$
 */

public abstract class AbstractRouterCollection implements UMORouterCollection {
    protected boolean matchAll = false;

    protected List routers = new CopyOnWriteArrayList();

    private RouterStatistics statistics;

    private UMORouterCatchAllStrategy catchAllStrategy;

    public AbstractRouterCollection(int type) {
        statistics = new RouterStatistics(type);
    }

    public void setRouters(List routers) {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();)
        {
            addRouter((UMORouter)iterator.next());
        }
    }

     public void addRouter(UMORouter router) {
        router.setRouterStatistics(getStatistics());
        routers.add(router);
    }

    public UMORouter removeRouter(UMORouter router) {
        if (routers.remove(router)) {
            return router;
        } else {
            return null;
        }
    }

    public List getRouters() {
        return routers;
    }


    public UMORouterCatchAllStrategy getCatchAllStrategy() {
        return catchAllStrategy;
    }

    public void setCatchAllStrategy(UMORouterCatchAllStrategy catchAllStrategy) {
        this.catchAllStrategy = catchAllStrategy;
        if(this.catchAllStrategy!=null && catchAllStrategy instanceof AbstractCatchAllStrategy) {
            ((AbstractCatchAllStrategy)this.catchAllStrategy).setStatistics(statistics);
        }
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public RouterStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(RouterStatistics stat) {
        this.statistics = stat;
    }

}