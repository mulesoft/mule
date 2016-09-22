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

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.routing.filters.EqualsFilter;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MessageFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testFilterPass() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_PAYLOAD), false, null);
    SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
    mp.setListener(listener);

    Event resultEvent = mp.process(testEvent());

    assertNotNull(listener.event);
    assertEquals(testEvent().getMessage(), resultEvent.getMessage());
  }

  @Test
  public void testFilterFail() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, null);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    mp.setListener(out);

    Event resultEvent = mp.process(testEvent());

    assertNull(out.event);
    assertNull(resultEvent);
  }

  @Test
  public void testFilterPassUnacceptedMP() throws Exception {
    MessageFilter mp = new MessageFilter(new EqualsFilter(TEST_PAYLOAD), false, null);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
    mp.setListener(out);
    mp.setUnacceptedMessageProcessor(unaccepted);

    Event resultEvent = mp.process(testEvent());

    assertNotNull(out.event);
    assertEquals(testEvent().getMessage(), resultEvent.getMessage());
    assertNull(unaccepted.event);
  }

  @Test
  public void testFilterFailUnacceptedMP() throws Exception {
    SensingNullMessageProcessor unaccepted = getSensingNullMessageProcessor();
    MessageFilter mp = new MessageFilter(new EqualsFilter(null), false, unaccepted);
    SensingNullMessageProcessor out = getSensingNullMessageProcessor();
    mp.setListener(out);

    Event inEvent = eventBuilder().message(InternalMessage.of(TEST_MESSAGE)).exchangePattern(ONE_WAY).build();

    Event resultEvent = mp.process(inEvent);

    assertNull(out.event);
    assertSame(VoidMuleEvent.getInstance(), resultEvent);
    assertNotNull(unaccepted.event);
  }
}
