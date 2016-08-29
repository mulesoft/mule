/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import static org.junit.Assert.assertEquals;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;

/**
 * Unit test for configuring message processors on an inbound endpoint.
 */
public class InboundEndpointMessageProcessorsTestCase extends AbstractMessageProcessorTestCase {

  private static final String TEST_MESSAGE = "test";

  private InboundEndpoint endpoint;
  private MuleMessage inMessage;
  private MuleEvent requestEvent;
  private MuleEvent result;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    inMessage = createTestRequestMessage();
    endpoint = createTestInboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);
    requestEvent = createTestRequestEvent(endpoint);
  }

  @Test
  public void testProcessors() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(muleContext);
    builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
    MessageProcessor mpChain = builder.build();

    result = mpChain.process(requestEvent);
    assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload());
  }

  @Test
  public void testNoProcessors() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(muleContext);
    MessageProcessor mpChain = builder.build();

    result = mpChain.process(requestEvent);
    assertEquals(TEST_MESSAGE, result.getMessage().getPayload());
  }

  protected MuleMessage createTestRequestMessage() {
    return MuleMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("prop1", "value1").build();
  }

  protected MuleEvent createTestRequestEvent(InboundEndpoint endpoint) throws Exception {
    Flow flow = getTestFlow();
    final MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(inMessage).flow(flow)
        .session(getTestSession(null, muleContext)).build();
    populateFieldsFromInboundEndpoint(event, endpoint);
    return event;
  }
}
