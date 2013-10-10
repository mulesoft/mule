/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
