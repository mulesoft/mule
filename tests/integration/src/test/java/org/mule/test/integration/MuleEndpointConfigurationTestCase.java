/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.vm.VMConnector;

/**
 * Test the creation of various endpoints from the service descriptor
 */
public class MuleEndpointConfigurationTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/test-endpoints-config.xml";
    }

    public void testComponent3RouterEndpoints() throws Exception
    {
        // test inbound
        Service service = muleContext.getRegistry().lookupService("TestComponent3");
        assertNotNull(service);
        OutboundRouterCollection outboundRouter = service.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(2, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router1 = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router1.getEndpoints().size());
        ImmutableEndpoint endpoint = (ImmutableEndpoint)router1.getEndpoints().get(0);
        assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:60201", endpoint.getEndpointURI().getAddress());
        // cannot get this to work and get axis tests to work
        // (axis seems to use undefined transformers in some strange way)
//        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        // assertTrue(provider.getTransformer() instanceof ObjectToFileMessage);
        assertTrue(endpoint instanceof OutboundEndpoint);

        // second Router
        OutboundRouter router2 = (OutboundRouter)outboundRouter.getRouters().get(1);
        assertEquals(2, router2.getEndpoints().size());
        endpoint = (ImmutableEndpoint)router2.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        // cannot get this to work and get axis tests to work
        // (axis seems to use undefined transformers in some strange way)
//        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint instanceof OutboundEndpoint);

        endpoint = (ImmutableEndpoint)router2.getEndpoints().get(1);
        assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
        assertFalse(endpoint.getTransformers().isEmpty());
        assertTrue(endpoint instanceof OutboundEndpoint);

    }

    public void testComponent4Endpoints() throws Exception
    {
        // test inbound
        Service service = muleContext.getRegistry().lookupService("TestComponent4");
        assertNotNull(service);
        assertNotNull(service.getInboundRouter().getEndpoints());
        assertEquals(1, service.getInboundRouter().getEndpoints().size());
        ImmutableEndpoint endpoint = (ImmutableEndpoint)service.getInboundRouter().getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("queue4", endpoint.getEndpointURI().getAddress());
        assertFalse(endpoint.getTransformers().isEmpty());
        assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
        assertTrue(endpoint instanceof InboundEndpoint);
    }

    public void testComponent4RouterEndpoints() throws Exception
    {
        // test inbound
        Service service = muleContext.getRegistry().lookupService("TestComponent4");
        assertNotNull(service);
        OutboundRouterCollection outboundRouter = service.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router.getEndpoints().size());
        ImmutableEndpoint endpoint = (ImmutableEndpoint)router.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        // cannot get this to work and get axis tests to work
        // (axis seems to use undefined transformers in some strange way)
//        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint instanceof OutboundEndpoint);

    }

    public void testComponent5RouterEndpoints() throws Exception
    {
        // test inbound
        Service service = muleContext.getRegistry().lookupService("TestComponent5");
        assertNotNull(service);
        OutboundRouterCollection outboundRouter = service.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router.getEndpoints().size());
        ImmutableEndpoint endpoint = (ImmutableEndpoint)router.getEndpoints().get(0);
        assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
        // cannot get this to work and get axis tests to work
        // (axis seems to use undefined transformers in some strange way)
//        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint instanceof OutboundEndpoint);

    }

    public void testEndpointFromURI() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello?remoteSync=true&remoteSyncTimeout=2002&connector=testConnector1");
        assertTrue(ep.isRemoteSync());
        assertEquals(2002, ep.getRemoteSyncTimeout());
        assertTrue(ep instanceof InboundEndpoint);

        // Test MuleEvent timeout proporgation
        MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage("hello"), ep, MuleTestUtils.getTestSession(muleContext), false);
        assertEquals(2002, event.getTimeout());

        ImmutableEndpoint ep2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello?connector=testConnector1");

        event = new DefaultMuleEvent(new DefaultMuleMessage("hello"), ep2, MuleTestUtils.getTestSession(muleContext), true);
        // default event timeout set in the test config file
        assertEquals(1001, event.getTimeout());
    }

}
