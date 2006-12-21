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
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMORouterCatchAllStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ForwardingCatchAllStrategy</code> acts as a catch and forward router for
 * any events not caught by the router this strategy is associated with. Users can
 * assign an endpoint to this strategy to forward all events to. This is similar to a
 * dead letter queue in messaging.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractCatchAllStrategy implements UMORouterCatchAllStrategy
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected UMOEndpoint endpoint;

    protected RouterStatistics statistics;

    public void setEndpoint(UMOEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public UMOEndpoint getEndpoint()
    {
        return endpoint;
    }

    public RouterStatistics getStatistics()
    {
        return statistics;
    }

    public void setStatistics(RouterStatistics statistics)
    {
        this.statistics = statistics;
    }

}
