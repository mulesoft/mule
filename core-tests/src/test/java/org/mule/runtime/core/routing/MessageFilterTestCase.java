/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.routing.filters.EqualsFilter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MessageFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testFilterPass() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_MESSAGE), false, null);
    SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
    mp.setListener(listener);

    Event inEvent = getTestEvent(TEST_MESSAGE);

    Event resultEvent = mp.process(inEvent);

    assertNotNull(listener.event);
    assertEquals(inEvent.getMessage(), resultEvent.getMessage());
  }

  @Test
  public void testFilterFail() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, null);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    mp.setListener(out);

    Event inEvent = getTestEvent(TEST_MESSAGE);

    Event resultEvent = mp.process(inEvent);

    assertNull(out.event);
    assertNull(resultEvent);
  }

  @Test
  public void testFilterPassUnacceptedMP() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_MESSAGE), false, null);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
    mp.setListener(out);
    mp.setUnacceptedMessageProcessor(unaccepted);

    Event inEvent = getTestEvent(TEST_MESSAGE);

    Event resultEvent = mp.process(inEvent);

    assertNotNull(out.event);
    assertEquals(inEvent.getMessage(), resultEvent.getMessage());
    assertNull(unaccepted.event);
  }

  @Test
  public void testFilterFailUnacceptedMP() throws Exception {
    SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
    MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, unaccepted);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    mp.setListener(out);
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    Event inEvent = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_MESSAGE))
        .exchangePattern(ONE_WAY)
        .build();

    Event resultEvent = mp.process(inEvent);

    assertNull(out.event);
    assertSame(VoidMuleEvent.getInstance(), resultEvent);
    assertNotNull(unaccepted.event);
  }
}
