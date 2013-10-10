/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.routing.outbound.MulticastingRouter;

/**
 * TODO
 */
public class OutRouteBuilder
{
    private MuleContext muleContext;
    private OutboundRouterCollection router;

    public OutRouteBuilder(OutboundRouterCollection router, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.router = router;
    }

    public OutRouteBuilder to(String uri)
    {
        MulticastingRouter mcr = new MulticastingRouter();
        mcr.setMuleContext(muleContext);
        router.addRoute(mcr);
        return this;
    }

    public OutRouteBuilder thenTo(String uri)
    {
        return null;
    }
}
