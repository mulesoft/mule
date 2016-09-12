/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

public class MessageChunkAggregatorTestCase extends AbstractMuleContextTestCase {

  public MessageChunkAggregatorTestCase() {
    setStartContext(true);
  }

  @Test
  public void testMessageProcessor() throws Exception {
    MuleSession session = getTestSession(null, muleContext);
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    MessageChunkAggregator router = new MessageChunkAggregator();
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    InternalMessage message1 = InternalMessage.of("test event A");
    InternalMessage message2 = InternalMessage.of("test event B");
    InternalMessage message3 = InternalMessage.of("test event C");

    EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");

    Event event1 = Event.builder(context).message(message1).groupCorrelation(new GroupCorrelation(3, null)).flow(getTestFlow())
        .session(session).build();
    Event event2 = Event.builder(context).message(message2).flow(getTestFlow()).session(session).build();
    Event event3 = Event.builder(context).message(message3).flow(getTestFlow()).session(session).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));
    Event resultEvent = router.process(event3);
    assertNotNull(resultEvent);
    InternalMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    String payload = getPayloadAsString(resultMessage);

    assertTrue(payload.contains("test event A"));
    assertTrue(payload.contains("test event B"));
    assertTrue(payload.contains("test event C"));
    assertTrue(payload.matches("test event [A,B,C]test event [A,B,C]test event [A,B,C]"));
  }
}
