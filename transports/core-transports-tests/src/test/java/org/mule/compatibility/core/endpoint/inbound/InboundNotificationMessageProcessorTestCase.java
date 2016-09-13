/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.context.notification.EndpointMessageNotification;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class InboundNotificationMessageProcessorTestCase extends AbstractMessageProcessorTestCase {

  @Test
  public void testProcess() throws Exception {
    TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
    muleContext.registerListener(listener);

    InboundEndpoint endpoint = createTestInboundEndpoint(null, null);
    Processor mp = new InboundNotificationMessageProcessor(endpoint);
    Event event = createTestInboundEvent(endpoint);
    mp.process(event);

    assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
    assertEquals(endpoint.getEndpointURI().getUri().toString(), listener.messageNotification.getEndpoint());
    assertTrue(listener.messageNotification.getSource() instanceof InternalMessage);
    assertThat(listener.messageNotification.getSource().getPayload().getValue(),
               equalTo(event.getMessage().getPayload().getValue()));
  }
}
