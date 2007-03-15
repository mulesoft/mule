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

import org.mule.management.stats.RouterStatistics;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.routing.UMORouter;
import org.mule.umo.routing.UMORouterCatchAllStrategy;
import org.mule.umo.routing.UMORouterCollection;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractRouterCollection</code> provides common method implementations of
 * router collections for in and outbound routers.
 */

public abstract class AbstractRouterCollection implements UMORouterCollection, Initialisable
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    protected boolean matchAll = false;

    protected List routers = new CopyOnWriteArrayList();

    private RouterStatistics statistics;

    private UMORouterCatchAllStrategy catchAllStrategy;

    public AbstractRouterCollection(int type)
    {
        statistics = new RouterStatistics(type);
    }


    public void initialise() throws InitialisationException
    {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();)
        {
            UMORouter router = (UMORouter) iterator.next();
            router.initialise();
        }
    }

    public void setRouters(List routers)
    {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();)
        {
            addRouter((UMORouter)iterator.next());
        }
    }

    public void addRouter(UMORouter router)
    {
        router.setRouterStatistics(getStatistics());
        routers.add(router);
    }

    public UMORouter removeRouter(UMORouter router)
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

    public List getRouters()
    {
        return routers;
    }

    public UMORouterCatchAllStrategy getCatchAllStrategy()
    {
        return catchAllStrategy;
    }

    public void setCatchAllStrategy(UMORouterCatchAllStrategy catchAllStrategy)
    {
        this.catchAllStrategy = catchAllStrategy;
        if (this.catchAllStrategy != null && catchAllStrategy instanceof AbstractCatchAllStrategy)
        {
            ((AbstractCatchAllStrategy)this.catchAllStrategy).setStatistics(statistics);
        }
    }

    public boolean isMatchAll()
    {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll)
    {
        this.matchAll = matchAll;
    }

    public RouterStatistics getStatistics()
    {
        return statistics;
    }

    public void setStatistics(RouterStatistics stat)
    {
        this.statistics = stat;
    }
}
