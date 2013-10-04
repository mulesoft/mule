/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.vm.VMConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the creation of various targets from the service descriptor
 */
public class MuleEndpointConfigurationTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/test-endpoints-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/test-endpoints-config-flow.xml"}});
    }

    public MuleEndpointConfigurationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testComponent3RouterEndpoints() throws Exception
    {
        Object serviceFlow = muleContext.getRegistry().lookupObject("TestComponent3");

        if (serviceFlow instanceof Service)
        {
            assertNotNull(serviceFlow);
            OutboundRouterCollection outboundRouter = (OutboundRouterCollection) ((Service) serviceFlow).getOutboundMessageProcessor();
            assertNotNull(outboundRouter);
            assertEquals(2, outboundRouter.getRoutes().size());
            // first Router
            OutboundRouter router1 = (OutboundRouter) outboundRouter.getRoutes().get(0);
            assertEquals(1, router1.getRoutes().size());
            ImmutableEndpoint endpoint = (ImmutableEndpoint) router1.getRoutes().get(0);
            assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("tcp://localhost:60201", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);

            // second Router
            OutboundRouter router2 = (OutboundRouter) outboundRouter.getRoutes().get(1);
            assertEquals(2, router2.getRoutes().size());
            endpoint = (ImmutableEndpoint) router2.getRoutes().get(0);
            assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);
            endpoint = (ImmutableEndpoint) router2.getRoutes().get(1);
            assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);

        }
        else if (serviceFlow instanceof Flow)
        {
            assertNotNull(serviceFlow);
            List<MessageProcessor> messageProcessors = ((Flow) serviceFlow).getMessageProcessors();

            assertNotNull(messageProcessors);
            assertEquals(2, messageProcessors.size());

            // <all> Router
            OutboundRouter allRouter = (OutboundRouter) messageProcessors.get(1);
            assertEquals(3, allRouter.getRoutes().size());
            ImmutableEndpoint endpoint = (ImmutableEndpoint) allRouter.getRoutes().get(0);
            assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("tcp://localhost:60201", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);

            endpoint = (ImmutableEndpoint) allRouter.getRoutes().get(1);
            assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);

            endpoint = (ImmutableEndpoint) allRouter.getRoutes().get(2);
            assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
            assertTrue(endpoint instanceof OutboundEndpoint);

        }
        else
        {
            fail("Unexpected Object");
        }
    }

    @Test
    public void testComponent4InboundEndpoint() throws Exception
    {
        Object serviceFlow = muleContext.getRegistry().lookupObject("TestComponent4");

        if (serviceFlow instanceof Service)
        {
            assertNotNull(serviceFlow);
            assertNotNull(((ServiceCompositeMessageSource) ((Service) serviceFlow).getMessageSource()).getEndpoints());
            assertEquals(1,
                ((ServiceCompositeMessageSource) ((Service) serviceFlow).getMessageSource()).getEndpoints()
                    .size());
            ImmutableEndpoint endpoint = ((ServiceCompositeMessageSource) ((Service) serviceFlow).getMessageSource()).getEndpoints()
                .get(0);
            assertNotNull(endpoint);
            assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("queue4", endpoint.getEndpointURI().getAddress());
            assertFalse(endpoint.getTransformers().isEmpty());
            assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
            assertTrue(endpoint instanceof InboundEndpoint);
        }
        else if (serviceFlow instanceof Flow)
        {
            assertNotNull(serviceFlow);
            assertNotNull(((Flow) serviceFlow).getMessageSource());
            assertEquals(1,
                ((DefaultInboundEndpoint) ((Flow) serviceFlow).getMessageSource()).getMessageProcessors()
                    .size());
            ImmutableEndpoint endpoint = ((DefaultInboundEndpoint) ((Flow) serviceFlow).getMessageSource());
            assertNotNull(endpoint);
            assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("queue4", endpoint.getEndpointURI().getAddress());
            assertFalse(endpoint.getTransformers().isEmpty());
            assertTrue(endpoint.getTransformers().get(0) instanceof ObjectToXml);
            assertTrue(endpoint instanceof InboundEndpoint);
        }
        else
        {
            fail("Unexpected Object");
        }
    }

    @Test
    public void testComponent4OutboundEndpoint() throws Exception
    {
        Object serviceFlow = muleContext.getRegistry().lookupObject("TestComponent4");
        if (serviceFlow instanceof Service)
        {
            assertNotNull(serviceFlow);
            OutboundRouterCollection outboundRouter = (OutboundRouterCollection) ((Service) serviceFlow).getOutboundMessageProcessor();
            assertNotNull(outboundRouter);
            assertEquals(1, outboundRouter.getRoutes().size());
            // first Router
            OutboundRouter router = (OutboundRouter) outboundRouter.getRoutes().get(0);
            assertEquals(1, router.getRoutes().size());
            ImmutableEndpoint endpoint = (ImmutableEndpoint) router.getRoutes().get(0);
            assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
            // cannot get this to work and get axis tests to work
            // (axis seems to use undefined transformers in some strange way)
            // assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
            assertTrue(endpoint instanceof OutboundEndpoint);
        }
        else if (serviceFlow instanceof Flow)
        {
            assertNotNull(serviceFlow);
            List<MessageProcessor> messageProcessors = ((Flow) serviceFlow).getMessageProcessors();
            assertNotNull(messageProcessors);

            ImmutableEndpoint endpoint = (ImmutableEndpoint) messageProcessors.get(1);
            assertEquals("udp", endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("udp://localhost:56731", endpoint.getEndpointURI().getAddress());
            // cannot get this to work and get axis tests to work
            // (axis seems to use undefined transformers in some strange way)
            // assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
            assertTrue(endpoint instanceof OutboundEndpoint);
        }
        else
        {
            fail("Unexpected Object");
        }
    }

    @Test
    public void testComponent5RouterEndpoints() throws Exception
    {
        Object serviceFlow = muleContext.getRegistry().lookupObject("TestComponent5");
        if (serviceFlow instanceof Service)
        {
            assertNotNull(serviceFlow);
            OutboundRouterCollection outboundRouter = (OutboundRouterCollection) ((Service) serviceFlow).getOutboundMessageProcessor();
            assertNotNull(outboundRouter);
            assertEquals(1, outboundRouter.getRoutes().size());
            // first Router
            OutboundRouter router = (OutboundRouter) outboundRouter.getRoutes().get(0);
            assertEquals(1, router.getRoutes().size());
            ImmutableEndpoint endpoint = (ImmutableEndpoint) router.getRoutes().get(0);
            assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
            // cannot get this to work and get axis tests to work
            // (axis seems to use undefined transformers in some strange way)
            // assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
            assertTrue(endpoint instanceof OutboundEndpoint);
        }
        else if (serviceFlow instanceof Flow)
        {
            assertNotNull(serviceFlow);
            List<MessageProcessor> messageProcessors = ((Flow) serviceFlow).getMessageProcessors();
            assertNotNull(messageProcessors);

            ImmutableEndpoint endpoint = (ImmutableEndpoint) messageProcessors.get(1);
            assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
            assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
            // cannot get this to work and get axis tests to work
            // (axis seems to use undefined transformers in some strange way)
            // assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
            assertTrue(endpoint instanceof OutboundEndpoint);
        }
        else
        {
            fail("Unexpected Object");
        }
    }

    @Test
    public void testEndpointFromURI() throws Exception
    {
        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://hello?exchangePattern=request-response&responseTimeout=2002&connector=testConnector1");
        assertEquals(MessageExchangePattern.REQUEST_RESPONSE, ep.getExchangePattern());
        assertEquals(2002, ep.getResponseTimeout());
        assertTrue(ep instanceof InboundEndpoint);

        // Test MuleEvent timeout proporgation
        MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage("hello", muleContext),
            (InboundEndpoint) ep, getTestService(), MuleTestUtils.getTestSession(muleContext));
        assertEquals(2002, event.getTimeout());

        ImmutableEndpoint ep2 = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://hello?connector=testConnector1");

        event = new DefaultMuleEvent(new DefaultMuleMessage("hello", muleContext), (InboundEndpoint) ep2,
            getTestService(), MuleTestUtils.getTestSession(muleContext));
        // default event timeout set in the test config file
        assertEquals(1001, event.getTimeout());
    }
}
