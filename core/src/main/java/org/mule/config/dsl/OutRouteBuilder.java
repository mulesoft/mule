/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.routing.outbound.MulticastingRouter;

/**
 * TODO
 */
@Deprecated
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
