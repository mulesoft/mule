/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.routing.OutboundRouterCatchAllStrategy;
import org.mule.api.routing.RouterStatisticsRecorder;
import org.mule.api.routing.RoutingException;
import org.mule.management.stats.RouterStatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <code>RouterCatchAllStrategy</code> is a strategy interface that allows developers to hook in custom code when
 * an event is being routed on the inbound or outbound but does not match any of the criteria defined for the routing.
 *
 * Think of catch all strategies as a safety net for your events to ensure that all events will get processed.  If you
 * do not use conditional routing logic, you will not need a catch all strategy.
 *
 * Note that it is advised to use this base class over the {@link org.mule.api.routing.OutboundRouterCatchAllStrategy} interface
 * so that the {@link org.mule.management.stats.RouterStatistics} are available.
 *
 * @see org.mule.routing.LoggingCatchAllStrategy
 * @see org.mule.routing.ForwardingCatchAllStrategy
 */
public abstract class AbstractCatchAllStrategy implements OutboundRouterCatchAllStrategy, RouterStatisticsRecorder
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /** Router statistics used to monitor if a catch all strategy is invoked and if any events are dispatched
     * from the strategy i.e. from the {@link org.mule.routing.ForwardingCatchAllStrategy}.
     */
    protected RouterStatistics statistics;


    public RouterStatistics getRouterStatistics()
    {
        return statistics;
    }

    public void setRouterStatistics(RouterStatistics statistics)
    {
        this.statistics = statistics;
    }

    /**
     * This method will be invoked when an event is received or being sent where the criteria of the router(s) do not
     * match the current event.
     *
     * @return A result message from this processing. Depending on the messaging style being used this might become the
     *         response message to a client or remote service call.
     * @throws org.mule.api.routing.RoutingException
     *          if there is a failure while processing this message.
     */
    public final MuleEvent process(MuleEvent event) throws RoutingException
    {
        RouterStatistics stats = getRouterStatistics();
        if(stats !=null && stats.isEnabled())
        {
            stats.incrementCaughtMessage();
        }
        else
        {
            logger.warn("Routing statistics not set on catch all strategy, this invocation will not be recorded.");
        }
        return doCatchMessage(event);
    }

    public abstract MuleEvent doCatchMessage(MuleEvent event) throws RoutingException;

}
