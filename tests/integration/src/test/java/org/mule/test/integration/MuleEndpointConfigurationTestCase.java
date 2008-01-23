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
import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformer.TransformerUtils;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.vm.VMConnector;

/**
 * Test the creation of various endpoints from the service descriptor
 */
public class MuleEndpointConfigurationTestCase extends FunctionalTestCase
{
    public MuleEndpointConfigurationTestCase()
    {
        super.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/test-endpoints-config.xml";
    }

    public void testComponent3RouterEndpoints() throws Exception
    {
        // test inbound
        Component component = muleContext.getRegistry().lookupComponent("TestComponent3");
        assertNotNull(component);
        OutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(2, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router1 = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router1.getEndpoints().size());
        Endpoint endpoint = (Endpoint)router1.getEndpoints().get(0);
        assertEquals("file", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("/C:/temp", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        // assertTrue(provider.getTransformer() instanceof ObjectToFileMessage);
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        // second Router
        OutboundRouter router2 = (OutboundRouter)outboundRouter.getRouters().get(1);
        assertEquals(2, router2.getEndpoints().size());
        endpoint = (Endpoint)router2.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (Endpoint)router2.getEndpoints().get(1);
        assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

    }

    public void testComponent4Endpoints() throws Exception
    {
        // test inbound
        Component component = muleContext.getRegistry().lookupComponent("TestComponent4");
        assertNotNull(component);
        assertNotNull(component.getInboundRouter().getEndpoints());
        assertEquals(1, component.getInboundRouter().getEndpoints().size());
        Endpoint endpoint = (Endpoint)component.getInboundRouter().getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("testEndpoint", endpoint.getName());
        assertEquals("queue4", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
        assertEquals(Endpoint.ENDPOINT_TYPE_RECEIVER, endpoint.getType());
    }

    public void testComponent4RouterEndpoints() throws Exception
    {
        // test inbound
        Component component = muleContext.getRegistry().lookupComponent("TestComponent4");
        assertNotNull(component);
        OutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        Endpoint endpoint = (Endpoint)router.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (Endpoint)router.getEndpoints().get(1);
        assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("yet.another.queue", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testComponent5RouterEndpoints() throws Exception
    {
        // test inbound
        Component component = muleContext.getRegistry().lookupComponent("TestComponent5");
        assertNotNull(component);
        OutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        OutboundRouter router = (OutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        Endpoint endpoint = (Endpoint)router.getEndpoints().get(0);
        assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (Endpoint)router.getEndpoints().get(1);
        assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45432", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(Endpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testEndpointFromURI() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello?remoteSync=true&remoteSyncTimeout=2002");
        assertTrue(ep.isRemoteSync());
        assertEquals(2002, ep.getRemoteSyncTimeout());
        assertEquals(Endpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());

        // Test MuleEvent timeout proporgation
        MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage("hello"), ep, MuleTestUtils.getTestSession(), false);
        assertEquals(2002, event.getTimeout());

        ImmutableEndpoint ep2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello");

        event = new DefaultMuleEvent(new DefaultMuleMessage("hello"), ep2, MuleTestUtils.getTestSession(), true);
        // default event timeout set in the test config file
        assertEquals(1001, event.getTimeout());
    }

}
