/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

public class MessageChunkAggregatorTestCase extends AbstractMuleContextTestCase {

  public MessageChunkAggregatorTestCase() {
    setStartContext(true);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Test
  public void testMessageProcessor() throws Exception {
    MuleSession session = new DefaultMuleSession();
    Flow flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    assertNotNull(flow);

    MessageChunkAggregator router = new MessageChunkAggregator();
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(router, true, muleContext);

    Message message1 = of("test event A");
    Message message2 = of("test event B");
    Message message3 = of("test event C");

    EventContext context = create(flow, TEST_CONNECTOR_LOCATION, "foo");

    CoreEvent event1 =
        InternalEvent.builder(context).message(message1).groupCorrelation(Optional.of(GroupCorrelation.of(0, 3)))
            .session(session).build();
    CoreEvent event2 = InternalEvent.builder(context).message(message2).session(session).build();
    CoreEvent event3 = InternalEvent.builder(context).message(message3).session(session).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));
    CoreEvent resultEvent = router.process(event3);
    assertNotNull(resultEvent);
    Message resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    String payload = getPayloadAsString(resultMessage);

    assertTrue(payload.contains("test event A"));
    assertTrue(payload.contains("test event B"));
    assertTrue(payload.contains("test event C"));
    assertTrue(payload.matches("test event [A,B,C]test event [A,B,C]test event [A,B,C]"));
  }
}
