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
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCatchAllStrategy;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.management.stats.RouterStatistics;

import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractRouterCollection</code> provides common method implementations of
 * router collections for in and outbound routers.
 */

public abstract class AbstractRouterCollection implements OutboundRouterCollection, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected final transient Log logger = LogFactory.getLog(getClass());

    protected boolean matchAll = false;

    @SuppressWarnings("unchecked")
    protected List<OutboundRouter> routers = new CopyOnWriteArrayList();

    protected RouterStatistics statistics;

    private OutboundRouterCatchAllStrategy catchAllStrategy;
    
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
        for (OutboundRouter router : routers)
        {
            router.dispose();
        }
    }

    public void setRouters(List<? extends OutboundRouter> routers)
    {
        for (OutboundRouter router : routers)
        {
            addRouter(router);
        }
    }

    public void addRouter(OutboundRouter router)
    {
        router.setRouterStatistics(getRouterStatistics());
        routers.add(router);
    }

    public OutboundRouter removeRouter(OutboundRouter router)
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

    public List<OutboundRouter> getRouters()
    {
        return routers;
    }

    public OutboundRouterCatchAllStrategy getCatchAllStrategy()
    {
        return catchAllStrategy;
    }

    public void setCatchAllStrategy(OutboundRouterCatchAllStrategy catchAllStrategy)
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

    public RouterStatistics getRouterStatistics()
    {
        return statistics;
    }

    public void setRouterStatistics(RouterStatistics stat)
    {
        this.statistics = stat;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
