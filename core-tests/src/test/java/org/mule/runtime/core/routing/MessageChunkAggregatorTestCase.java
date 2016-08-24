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
import static org.mule.runtime.core.DefaultMessageContext.create;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.Correlation;
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

    MuleMessage message1 = MuleMessage.of("test event A");
    MuleMessage message2 = MuleMessage.of("test event B");
    MuleMessage message3 = MuleMessage.of("test event C");

    MessageContext context = create(flow, "test", "foo");

    DefaultMuleEvent event1 =
        new DefaultMuleEvent(context, message1, getTestFlow(), session);
    event1.setCorrelation(new Correlation(1, null));
    DefaultMuleEvent event2 =
        new DefaultMuleEvent(context, message2, getTestFlow(), session);
    event1.setCorrelation(new Correlation(2, null));
    DefaultMuleEvent event3 =
        new DefaultMuleEvent(context, message3, getTestFlow(), session);
    event1.setCorrelation(new Correlation(3, null));

    assertNull(router.process(event1));
    assertNull(router.process(event2));
    MuleEvent resultEvent = router.process(event3);
    assertNotNull(resultEvent);
    MuleMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    String payload = getPayloadAsString(resultMessage);

    assertTrue(payload.contains("test event A"));
    assertTrue(payload.contains("test event B"));
    assertTrue(payload.contains("test event C"));
    assertTrue(payload.matches("test event [A,B,C]test event [A,B,C]test event [A,B,C]"));
  }
}
