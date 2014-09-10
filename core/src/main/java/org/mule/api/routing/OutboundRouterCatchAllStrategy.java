/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
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
