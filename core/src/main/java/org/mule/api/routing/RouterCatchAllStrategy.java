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
import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * <code>RouterCatchAllStrategy</code> TODO
 */

public interface RouterCatchAllStrategy
{
    void setEndpoint(ImmutableEndpoint endpoint);

    ImmutableEndpoint getEndpoint();

    MuleMessage catchMessage(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException;
}
