/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.MalformedEndpointException;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.DynamicOutboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import org.junit.Test;

public class DynamicEndpointParsingTestCase extends AbstractMuleContextEndpointTestCase {

  public DynamicEndpointParsingTestCase() {
    setStartContext(true);
  }

  @Test
  public void testDynamicEventMessageSourceURIUntouched() throws Exception {
    OutboundEndpoint endpoint = createRequestResponseEndpoint("test://localhost:#[message.outboundProperties.port]");
    assertTrue(endpoint instanceof DynamicOutboundEndpoint);

    MuleEvent event = getTestEvent("test", getTestInboundEndpoint("test1"));
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("port", 12345).build());

    MuleEvent response = endpoint.process(event);

    assertEquals("test://test", response.getMessageSourceURI().toString());
  }

  @Test(expected = MalformedEndpointException.class)
  public void testExpressionInSchemeIsForbidden() throws Exception {
    createRequestResponseEndpoint("#[message.outboundProperties.scheme]://#[message.outboundProperties.host]:#[message.outboundProperties:port]");
  }

  @Test(expected = MalformedEndpointException.class)
  public void testMalformedExpressionInUriIsDetected() throws Exception {
    createRequestResponseEndpoint("test://#[message.outboundProperties.host:#[message.outboundProperties.port]");
  }

  @Test(expected = MalformedEndpointException.class)
  public void testDynamicInboundEndpointNotAllowed() throws Exception {
    EndpointURIEndpointBuilder endpointBuilder =
        new EndpointURIEndpointBuilder("test://#[message.outboundProperties.host]:#[message.outboundProperties.port]",
                                       muleContext);
    endpointBuilder.buildInboundEndpoint();
  }

  @Test
  public void testMEPOverridingInUri() throws Exception {
    OutboundEndpoint endpoint = createEndpoint("test://#[message.outboundProperties.host]:#[message.outboundProperties.port]",
                                               MessageExchangePattern.ONE_WAY);

    assertTrue(endpoint instanceof DynamicOutboundEndpoint);

    MuleEvent event = getTestEvent("test", getTestInboundEndpoint("test1"));
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("port", 12345)
        .addOutboundProperty("host", "localhost").build());

    MuleEvent response = endpoint.process(event);
    assertSame(VoidMuleEvent.getInstance(), response);

    // Now test set on the endpoint
    endpoint =
        createRequestResponseEndpoint("test://#[message.outboundProperties.host]:#[message.outboundProperties.port]?exchangePattern=REQUEST_RESPONSE");

    assertTrue(endpoint instanceof DynamicOutboundEndpoint);

    event = getTestEvent("test", getTestInboundEndpoint("test1"));
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("port", 12345)
        .addOutboundProperty("host", "localhost").build());

    response = endpoint.process(event);
    assertNotNull(response);
    assertEquals(MessageExchangePattern.REQUEST_RESPONSE, endpoint.getExchangePattern());
  }

  protected OutboundEndpoint createRequestResponseEndpoint(String uri) throws EndpointException, InitialisationException {
    return createEndpoint(uri, MessageExchangePattern.REQUEST_RESPONSE);
  }

  private OutboundEndpoint createEndpoint(String uri, MessageExchangePattern exchangePattern)
      throws EndpointException, InitialisationException {
    EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
    endpointBuilder.setExchangePattern(exchangePattern);

    return endpointBuilder.buildOutboundEndpoint();
  }

}
