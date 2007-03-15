/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;

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
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("TestComponent3");
        assertNotNull(descriptor);
        UMOOutboundRouterCollection outboundRouter = descriptor.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(2, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router1 = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(1, router1.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router1.getEndpoints().get(0);
        assertEquals("file", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("/C:/temp", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformer());
        // assertTrue(provider.getTransformer() instanceof ObjectToFileMessage);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        // second Router
        UMOOutboundRouter router2 = (UMOOutboundRouter)outboundRouter.getRouters().get(1);
        assertEquals(2, router2.getEndpoints().size());
        endpoint = (UMOEndpoint)router2.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertNull(endpoint.getTransformer());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router2.getEndpoints().get(1);
        assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
        assertNull(endpoint.getTransformer());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

    }

    public void testComponent4Endpoints() throws Exception
    {
        // test inbound
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("TestComponent4");
        assertNotNull(descriptor);
        assertNotNull(descriptor.getInboundRouter().getEndpoints());
        assertEquals(1, descriptor.getInboundRouter().getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)descriptor.getInboundRouter().getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("vm", endpoint.getConnector().getProtocol().toLowerCase());
        assertTrue(endpoint.getName().equals("testEndpoint"));
        assertEquals("queue4", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformer());
        assertTrue(endpoint.getTransformer() instanceof ObjectToXml);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER, endpoint.getType());
    }

    public void testComponent4RouterEndpoints() throws Exception
    {
        // test inbound
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("TestComponent4");
        assertNotNull(descriptor);
        UMOOutboundRouterCollection outboundRouter = descriptor.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
        assertNull(endpoint.getTransformer());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router.getEndpoints().get(1);
        assertEquals("vm", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("yet.another.queue", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformer());
        assertTrue(endpoint.getTransformer() instanceof ObjectToXml);
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testComponent5RouterEndpoints() throws Exception
    {
        // test inbound
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("TestComponent5");
        assertNotNull(descriptor);
        UMOOutboundRouterCollection outboundRouter = descriptor.getOutboundRouter();
        assertNotNull(outboundRouter);
        assertEquals(1, outboundRouter.getRouters().size());
        // first Router
        UMOOutboundRouter router = (UMOOutboundRouter)outboundRouter.getRouters().get(0);
        assertEquals(2, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
        assertNull(endpoint.getTransformer());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());

        endpoint = (UMOEndpoint)router.getEndpoints().get(1);
        assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
        assertEquals("tcp://localhost:45432", endpoint.getEndpointURI().getAddress());
        assertNull(endpoint.getTransformer());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_SENDER, endpoint.getType());
    }

    public void testEndpointFromURI() throws Exception
    {
        MuleEndpoint ep = new MuleEndpoint("test://hello?remoteSync=true&remoteSyncTimeout=2002", true);
        ep.setManagementContext(managementContext);
        ep.initialise();
        assertTrue(ep.isRemoteSync());
        assertEquals(2002, ep.getRemoteSyncTimeout());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());

        // Test Event timeout proporgation
        UMOEvent event = new MuleEvent(new MuleMessage("hello"), ep, MuleTestUtils.getTestSession(), false);
        assertEquals(2002, event.getTimeout());

        event = new MuleEvent(new MuleMessage("hello"), new MuleEndpoint("test://hello", true),
            MuleTestUtils.getTestSession(), true);
        // default event timeout set in the test config file
        assertEquals(1001, event.getTimeout());
    }

}
