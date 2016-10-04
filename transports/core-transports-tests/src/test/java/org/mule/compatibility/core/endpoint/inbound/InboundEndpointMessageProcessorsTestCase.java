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
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;

/**
 * Unit test for configuring message processors on an inbound endpoint.
 */
public class InboundEndpointMessageProcessorsTestCase extends AbstractMessageProcessorTestCase {

  private static final String TEST_MESSAGE = "test";

  private InboundEndpoint endpoint;
  private InternalMessage inMessage;
  private Event requestEvent;
  private Event result;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    inMessage = createTestRequestMessage();
    endpoint = createTestInboundEndpoint(null, null, null, null, MessageExchangePattern.REQUEST_RESPONSE, null);
    requestEvent = createTestRequestEvent(endpoint);
  }

  @Test
  public void testProcessors() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
    Processor mpChain = builder.build();

    result = process(mpChain, requestEvent);
    assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload().getValue());
  }

  @Test
  public void testNoProcessors() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    Processor mpChain = builder.build();

    result = process(mpChain, requestEvent);
    assertEquals(TEST_MESSAGE, result.getMessage().getPayload().getValue());
  }

  protected InternalMessage createTestRequestMessage() {
    return InternalMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("prop1", "value1").build();
  }

  protected Event createTestRequestEvent(InboundEndpoint endpoint) throws Exception {
    final Event event = eventBuilder().message(inMessage).session(new DefaultMuleSession()).build();
    return populateFieldsFromInboundEndpoint(event, endpoint);
  }
}
