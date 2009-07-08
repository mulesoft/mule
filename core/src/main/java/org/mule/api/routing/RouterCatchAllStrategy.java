/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;

/**
 * <code>RouterCatchAllStrategy</code> is a strategy interface that allows developers to hook in custom code when
 * an event is being routed on the inbound or outbound but does not match any of the criteria defined for the routing.
 *
 * Think of catch all strategies as a safety net for your events to ensure that all events will get processed.  If you
 * do not use conditional routing logic, you will not need a catch all strategy.
 *
 */
public interface RouterCatchAllStrategy
{
    /**
     * This method will be invoked when an event is received or being sent where the criteria of the router(s) do not
     * match the current event.
     *
     * @param message the current message being processed
     * @param session the current session
     * @return A result message from this processing. Depending on the messaging style being used this might become the
     * response message to a client or remote service call.
     * @throws RoutingException if there is a failure while processing this message.
     */
    MuleMessage catchMessage(MuleMessage message, MuleSession session) throws RoutingException;
}
