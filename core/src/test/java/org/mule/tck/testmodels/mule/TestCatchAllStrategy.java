/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.OutboundRouterCatchAllStrategy;
import org.mule.api.routing.RoutingException;
import org.mule.util.StringMessageUtils;

public class TestCatchAllStrategy implements OutboundRouterCatchAllStrategy
{
    private OutboundEndpoint endpoint;

    private String testProperty;

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public OutboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    public MuleEvent process(MuleEvent event)
        throws RoutingException
    {
        System.out.println(StringMessageUtils.getBoilerPlate("Caught an event in the router!", '*', 40));
        return null;
    }

    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }
}
