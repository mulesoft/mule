/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.routing.Router;
import org.mule.api.service.Service;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.routing.inbound.IdempotentSecureHashReceiver;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.tck.FunctionalTestCase;

import java.util.List;

public class CoreNamespaceRoutersTestCase extends FunctionalTestCase
{
    public String getConfigResources()
    {
        return "core-namespace-routers.xml";
    }

    public void testForwardingRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("ForwardingRouter");
        assertTrue(r instanceof ForwardingConsumer);
    }

    public void testIdempotentSecureHashReceiverRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("IdempotentSecureHashReceiverRouter");
        assertTrue(r instanceof IdempotentSecureHashReceiver);
    }

    public void testInboundPassThroughRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("InboundPassThroughRouter");
        assertTrue(r instanceof InboundPassThroughRouter);
    }

    public void testSelectiveConsumerRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("SelectiveConsumerRouter");
        assertTrue(r instanceof SelectiveConsumer);
        assertFalse(((SelectiveConsumer) r).isTransformFirst());
    }

    protected Router lookupInboundRouterFromService(String serviceName) throws Exception
    {
        Service c = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(c);
        List routers = c.getInboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0) instanceof Router);
        return (Router) routers.get(0);
    }
}
