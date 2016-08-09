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

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.DefaultInboundEndpoint;
import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.compatibility.transport.vm.VMConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.module.xml.transformer.ObjectToXml;
import org.mule.tck.MuleEndpointTestUtils;

import java.util.List;

import org.junit.Test;

/**
 * Test the creation of various targets from the service descriptor
 */
public class MuleEndpointConfigurationFlowTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/test-endpoints-config-flow.xml";
  }

  @Test
  public void testComponent3RouterEndpoints() throws Exception {
    Object flow = muleContext.getRegistry().lookupObject("TestComponent3");

    assertNotNull(flow);
    List<MessageProcessor> messageProcessors = ((Flow) flow).getMessageProcessors();

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
    assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
    assertEquals("tcp://localhost:56731", endpoint.getEndpointURI().getAddress());
    assertTrue(endpoint instanceof OutboundEndpoint);

    endpoint = (ImmutableEndpoint) allRouter.getRoutes().get(2);
    assertEquals("test", endpoint.getConnector().getProtocol().toLowerCase());
    assertEquals("test.queue2", endpoint.getEndpointURI().getAddress());
    assertTrue(endpoint instanceof OutboundEndpoint);
  }

  @Test
  public void testComponent4InboundEndpoint() throws Exception {
    Object flow = muleContext.getRegistry().lookupObject("TestComponent4");

    assertNotNull(flow);
    assertNotNull(((Flow) flow).getMessageSource());

    assertEquals(1, ((DefaultInboundEndpoint) ((Flow) flow).getMessageSource()).getMessageProcessors().size());

    ImmutableEndpoint endpoint = ((DefaultInboundEndpoint) ((Flow) flow).getMessageSource());
    assertNotNull(endpoint);
    assertEquals(VMConnector.VM, endpoint.getConnector().getProtocol().toLowerCase());
    assertEquals("queue4", endpoint.getEndpointURI().getAddress());
    assertFalse(endpoint.getMessageProcessors().isEmpty());
    assertTrue(endpoint.getMessageProcessors().get(0) instanceof ObjectToXml);
    assertTrue(endpoint instanceof InboundEndpoint);
  }

  @Test
  public void testComponent4OutboundEndpoint() throws Exception {
    Object flow = muleContext.getRegistry().lookupObject("TestComponent4");

    assertNotNull(flow);
    List<MessageProcessor> messageProcessors = ((Flow) flow).getMessageProcessors();
    assertNotNull(messageProcessors);

    ImmutableEndpoint endpoint = (ImmutableEndpoint) messageProcessors.get(1);
    assertEquals("tcp", endpoint.getConnector().getProtocol().toLowerCase());
    assertEquals("tcp://localhost:56731", endpoint.getEndpointURI().getAddress());
    assertTrue(endpoint instanceof OutboundEndpoint);
  }

  @Test
  public void testComponent5RouterEndpoints() throws Exception {
    Object flow = muleContext.getRegistry().lookupObject("TestComponent5");

    assertNotNull(flow);
    List<MessageProcessor> messageProcessors = ((Flow) flow).getMessageProcessors();
    assertNotNull(messageProcessors);

    ImmutableEndpoint endpoint = (ImmutableEndpoint) messageProcessors.get(1);
    assertEquals(TcpConnector.TCP, endpoint.getConnector().getProtocol().toLowerCase());
    assertEquals("tcp://localhost:45431", endpoint.getEndpointURI().getAddress());
    assertTrue(endpoint instanceof OutboundEndpoint);
  }

  @Test
  public void testEndpointFromURI() throws Exception {
    ImmutableEndpoint ep = getEndpointFactory()
        .getInboundEndpoint("test://hello?exchangePattern=request-response&responseTimeout=2002&connector=testConnector1");
    assertEquals(MessageExchangePattern.REQUEST_RESPONSE, ep.getExchangePattern());
    assertEquals(2002, ep.getResponseTimeout());
    assertTrue(ep instanceof InboundEndpoint);

    // Test MuleEvent timeout proporgation
    MuleEvent event = MuleEndpointTestUtils.getTestEvent("hello", getTestFlow(), (InboundEndpoint) ep, muleContext);
    assertEquals(2002, event.getTimeout());

    ImmutableEndpoint ep2 = getEndpointFactory().getInboundEndpoint("test://hello?connector=testConnector1");

    event = MuleEndpointTestUtils.getTestEvent("hello", getTestFlow(), (InboundEndpoint) ep2, muleContext);
    // default event timeout set in the test config file
    assertEquals(1001, event.getTimeout());
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
