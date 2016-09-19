/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.AbstractEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.session.DefaultMuleSession;

import org.junit.Test;

public class OutboundResponsePropertiesMessageProcessorTestCase extends AbstractMessageProcessorTestCase {

  private static String MY_PROPERTY_KEY = "myProperty";
  private static String MY_PROPERTY_VAL = "myPropertyValue";

  @Test
  public void testProcess() throws Exception {
    OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
    InterceptingMessageProcessor mp = new OutboundResponsePropertiesMessageProcessor(endpoint);
    mp.setListener(event -> {
      // return event with same payload but no properties
      try {
        Flow flow = getTestFlow();
        return Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
            .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue()).build())
            .exchangePattern(REQUEST_RESPONSE)
            .flow(flow).session(new DefaultMuleSession()).build();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    Event event = createTestOutboundEvent();
    event = Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(MY_PROPERTY_KEY, MY_PROPERTY_VAL).build())
        .build();

    Event result = mp.process(event);

    assertNotNull(result);
    assertThat(result.getMessageAsString(muleContext), is(TEST_MESSAGE));
    assertThat(result.getMessage().getOutboundProperty(MY_PROPERTY_KEY), is(MY_PROPERTY_VAL));
  }

  @Override
  protected void customizeEndpointBuilder(EndpointBuilder endpointBuilder) {
    endpointBuilder.setProperty(AbstractEndpointBuilder.PROPERTY_RESPONSE_PROPERTIES, "myProperty");
  }
}
