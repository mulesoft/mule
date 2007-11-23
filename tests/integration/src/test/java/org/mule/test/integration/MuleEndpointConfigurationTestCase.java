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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.providers.vm.VMConnector;
import org.mule.providers.tcp.TcpConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.TransformerUtils;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;

import sun.rmi.transport.tcp.TCPConnection;

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
        UMOComponent component = managementContext.getRegistry().lookupComponent("TestComponent3");
        assertNotNull(component);
        UMOOutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(2, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router1 = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router1.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router1.getEndpoints().get(0);
        assertEquals("file", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("/C:/temp", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        // assertTrue(provider.getTransformer() instanceof ObjectToFileMessage);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        // second Router
        UMOOutboundRouter router2 = (UMOOutboundRouter)outboundRouter.getRouters().get(1);
        assertEquals(2, router2.getEndpoints().size());
        endpoint = (UMOEndpoint)router2.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router2.getEndpoints().get(1);
        assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

    }

    public void testComponent4Endpoints() throws Exception
    {
        // test inbound
        UMOComponent component = managementContext.getRegistry().lookupComponent("TestComponent4");
        assertNotNull(component);
        assertNotNull(component.getInboundRouter().getEndpoints());
        assertEquals(1, component.getInboundRouter().getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)component.getInboundRouter().getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("testEndpoint", endpoint.getName());
        assertEquals("queue4", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER, endpoint.getType());
    }

    public void testComponent4RouterEndpoints() throws Exception
    {
        // test inbound
        UMOComponent component = managementContext.getRegistry().lookupComponent("TestComponent4");
        assertNotNull(component);
        UMOOutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router.getEndpoints().get(1);
        assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("yet.another.queue", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testComponent5RouterEndpoints() throws Exception
    {
        // test inbound
        UMOComponent component = managementContext.getRegistry().lookupComponent("TestComponent5");
        assertNotNull(component);
        UMOOutboundRouterCollection outboundRouter = component.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router.getEndpoints().get(1);
        assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45432", endpoint.getEndpointURI().getAddress());
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers())); 
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testEndpointFromURI() throws Exception
    {
        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello?remoteSync=true&remoteSyncTimeout=2002");
        assertTrue(ep.isRemoteSync());
        assertEquals(2002, ep.getRemoteSyncTimeout());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());

        // Test Event timeout proporgation
        UMOEvent event = new MuleEvent(new MuleMessage("hello"), ep, MuleTestUtils.getTestSession(), false);
        assertEquals(2002, event.getTimeout());

        UMOImmutableEndpoint ep2 = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://hello");

        event = new MuleEvent(new MuleMessage("hello"), ep2, MuleTestUtils.getTestSession(), true);
        // default event timeout set in the test config file
        assertEquals(1001, event.getTimeout());
    }

}
