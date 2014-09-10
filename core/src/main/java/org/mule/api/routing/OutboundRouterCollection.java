/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.management.stats.RouterStatistics;

import java.util.List;

/**
 * <code>RouterCollection</code> defines the interface for a MessageRouter that
 * manages more than one router. A {@link OutboundRouterCatchAllStrategy} can be set
 * on this router to route unwanted or unfiltered events. If a catch strategy is not
 * set, the router just returns null. <code>OutboundRouterCollection</code> is
 * responsible for holding all outbound routers for a service service.
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
public interface OutboundRouterCollection
    extends MatchingRouter, RouterStatisticsRecorder, Initialisable, Disposable, MuleContextAware
{
    List<MatchableMessageProcessor> getRoutes();

    OutboundRouterCatchAllStrategy getCatchAllStrategy();

    void setCatchAllStrategy(OutboundRouterCatchAllStrategy catchAllStrategy);

    boolean isMatchAll();

    void setMatchAll(boolean matchAll);

    /**
     * Determines if any targets have been set on this router.
     */
    boolean hasEndpoints();

    RouterStatistics getRouterStatistics();
}
