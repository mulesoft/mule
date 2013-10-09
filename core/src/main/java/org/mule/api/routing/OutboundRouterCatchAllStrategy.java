/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

/**
 * <code>RouterCatchAllStrategy</code> is a strategy interface that allows developers to hook in custom code when
 * an event is being routed on the inbound or outbound but does not match any of the criteria defined for the routing.
 *
 * Think of catch all strategies as a safety net for your events to ensure that all events will get processed.  If you
 * do not use conditional routing logic, you will not need a catch all strategy.
 *
 */
public interface OutboundRouterCatchAllStrategy extends MessageProcessor
{
    /**
     * This method will be invoked when an event is received or being sent where the criteria of the router(s) do not
     * match the current event.
     * @param event the current event being processed
     *
     * @return A result message from this processing. Depending on the messaging style being used this might become the
     * response message to a client or remote service call.
     * @throws RoutingException if there is a failure while processing this message.
     */
    MuleEvent process(MuleEvent event) throws RoutingException;
}
