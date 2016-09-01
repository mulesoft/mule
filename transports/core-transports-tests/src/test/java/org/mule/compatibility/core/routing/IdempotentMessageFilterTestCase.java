/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import org.junit.Test;

public class IdempotentMessageFilterTestCase extends AbstractMuleContextEndpointTestCase {

  @Test
  public void testIdempotentReceiver() throws Exception {
    Flow flow2 = getTestFlow();
    Flow flow = flow2;

    MuleSession session = mock(MuleSession.class);

    InboundEndpoint endpoint1 = getTestInboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=one-way");

    IdempotentMessageFilter ir = new IdempotentMessageFilter();
    ir.setIdExpression("#[message.inboundProperties.id]");
    ir.setFlowConstruct(flow);
    ir.setThrowOnUnaccepted(false);
    ir.setStorePrefix("foo");
    ir.setStore(new InMemoryObjectStore<String>());

    MuleMessage okMessage = MuleMessage.builder().payload("OK").addOutboundProperty("id", "1").build();
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(okMessage).flow(flow2)
        .session(session).build();
    populateFieldsFromInboundEndpoint(event, endpoint1);

    // This one will process the event on the target endpoint
    MuleEvent processedEvent = ir.process(event);
    assertNotNull(processedEvent);

    // This will not process, because the ID is a duplicate
    okMessage = MuleMessage.builder().payload("OK").addOutboundProperty("id", "1").build();
    event = MuleEvent.builder(DefaultMessageContext.create(flow2, TEST_CONNECTOR)).message(okMessage).flow(flow2).session(session)
        .build();
    populateFieldsFromInboundEndpoint(event, endpoint1);
    processedEvent = ir.process(event);
    assertNull(processedEvent);
  }
}
