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

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;

/**
 * <code>OutboundRouterCollection</code> is responsible for holding all outbound routers for a service service.
 */

public interface OutboundRouterCollection extends RouterCollection
{
    /**
     * Prepares one or more events to be dispached by a Message Dispatcher.
     * 
     * @param message The source Message
     * @param session The current session
     * @return a list containing 0 or events to be dispatched
     * @throws RoutingException If any of the events cannot be created.
     */
    MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException;

    /**
     * Determines if any endpoints have been set on this router.
     * 
     * @return
     */
    boolean hasEndpoints();
}
