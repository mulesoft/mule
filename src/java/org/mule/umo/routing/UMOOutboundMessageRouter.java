/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;

/**
 * <code>UMOOutboundMessageRouter</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOOutboundMessageRouter extends UMORouterCollection
{
    /**
     * Prepares one or more events to be dispached by a Message Dispatcher
     * @param message The source Message
     * @param session The current session
     * @return a list containing 0 or events to be dispatched
     * @throws RoutingException If any of the events cannot be created.
     */

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException;
}
