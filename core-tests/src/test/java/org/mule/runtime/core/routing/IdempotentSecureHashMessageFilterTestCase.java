/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class IdempotentSecureHashMessageFilterTestCase extends AbstractMuleContextTestCase {

  public IdempotentSecureHashMessageFilterTestCase() {
    setStartContext(true);
  }

  @Test
  public void testIdempotentReceiver() throws Exception {
    Flow flow = getTestFlow(muleContext);

    MuleSession session = mock(MuleSession.class);

    IdempotentSecureHashMessageFilter ir = new IdempotentSecureHashMessageFilter();
    ir.setFlowConstruct(flow);
    ir.setStorePrefix("foo");
    ir.setObjectStore(new InMemoryObjectStore<>());
    ir.setMuleContext(muleContext);

    final EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION);

    Message okMessage = of("OK");
    Event event = Event.builder(context).message(okMessage).flow(getTestFlow(muleContext)).session(session).build();

    // This one will process the event on the target endpoint
    Event processedEvent = ir.process(event);
    assertNotNull(processedEvent);

    // This will not process, because the message is a duplicate
    okMessage = of("OK");
    event = Event.builder(context).message(okMessage).flow(getTestFlow(muleContext)).session(session).build();
    processedEvent = ir.process(event);
    assertNull(processedEvent);

    // This will process, because the message is not a duplicate
    okMessage = of("Not OK");
    event = Event.builder(context).message(okMessage).flow(getTestFlow(muleContext)).session(session).build();
    processedEvent = ir.process(event);
    assertNotNull(processedEvent);
  }
}
