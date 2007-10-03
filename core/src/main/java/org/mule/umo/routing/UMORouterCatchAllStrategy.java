/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>UMORouterCatchAllStrategy</code> TODO
 */

public interface UMORouterCatchAllStrategy
{
    void setEndpoint(UMOImmutableEndpoint endpoint);

    UMOImmutableEndpoint getEndpoint();

    UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException;
}
