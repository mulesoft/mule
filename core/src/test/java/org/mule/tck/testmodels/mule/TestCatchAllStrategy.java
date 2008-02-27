/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RouterCatchAllStrategy;
import org.mule.api.routing.RoutingException;
import org.mule.util.StringMessageUtils;

public class TestCatchAllStrategy implements RouterCatchAllStrategy
{
    private OutboundEndpoint endpoint;

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public OutboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    public MuleMessage catchMessage(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException
    {
        System.out.println(StringMessageUtils.getBoilerPlate("Caught an event in the router!", '*', 40));
        return null;
    }
}
