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
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.routing.Router;
import org.mule.api.routing.RouterCatchAllStrategy;
import org.mule.api.routing.RouterCollection;
import org.mule.management.stats.RouterStatistics;

import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractRouterCollection</code> provides common method implementations of
 * router collections for in and outbound routers.
 */

public abstract class AbstractRouterCollection implements RouterCollection, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected final transient Log logger = LogFactory.getLog(getClass());

    protected boolean matchAll = false;

    protected List routers = new CopyOnWriteArrayList();

    private RouterStatistics statistics;

    private RouterCatchAllStrategy catchAllStrategy;
    
    protected MuleContext muleContext;

    public AbstractRouterCollection(int type)
    {
        statistics = new RouterStatistics(type);
    }

    public void initialise() throws InitialisationException
    {
        LifecycleTransitionResult.initialiseAll(routers.iterator());
    }

    public void dispose()
    {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();)
        {
            Router router = (Router) iterator.next();
            router.dispose();
        }
    }

    public void setRouters(List routers)
    {
        for (Iterator iterator = routers.iterator(); iterator.hasNext();)
        {
            addRouter((Router) iterator.next());
        }
    }

    public void addRouter(Router router)
    {
        router.setRouterStatistics(getStatistics());
        routers.add(router);
    }

    public Router removeRouter(Router router)
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

    public RouterCatchAllStrategy getCatchAllStrategy()
    {
        return catchAllStrategy;
    }

    public void setCatchAllStrategy(RouterCatchAllStrategy catchAllStrategy)
    {
        this.catchAllStrategy = catchAllStrategy;
        if (this.catchAllStrategy != null && catchAllStrategy instanceof AbstractCatchAllStrategy)
        {
            ((AbstractCatchAllStrategy) this.catchAllStrategy).setStatistics(statistics);
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

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
