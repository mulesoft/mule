/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
 */

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
