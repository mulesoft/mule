/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.work;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.Event.setCurrentEvent;

import org.mule.compatibility.core.work.AbstractMuleEventWork;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test case to reproduce issue described in MULE-4407 and validate fix.
 */
public class MuleEventWorkTestCase extends AbstractMuleContextTestCase {

  protected Event originalEvent;
  protected Latch latch = new Latch();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    // Create a dummy event and give it some properties
    originalEvent = eventBuilder().message(InternalMessage.builder().payload("test").addOutboundProperty("test", "val")
        .addOutboundProperty("test2", "val2").build()).build();
    setCurrentEvent(originalEvent);
  }

  @Test
  public void testScheduleMuleEventWork() throws Exception {
    muleContext.getSchedulerService().cpuIntensiveScheduler().submit(new TestMuleEventWork(originalEvent));

    assertTrue("Timed out waiting for latch", latch.await(2000, TimeUnit.MILLISECONDS));

    assertSame(originalEvent, getCurrentEvent());
  }

  @Test
  public void testRunMuleEventWork() throws Exception {
    new TestMuleEventWork(originalEvent).run();

    // NOTE: This assertion documents/tests current behaviour but does not seem
    // correct.
    // In scenarios where Work implementations are run in the same thread rather
    // than being scheduled then the RequestContext ThreadLocal value is
    // overwritten with a new copy which is not desirable.
    // See: MULE-4409
    assertNotSame(originalEvent, getCurrentEvent());
  }

  private class TestMuleEventWork extends AbstractMuleEventWork {

    public TestMuleEventWork(Event event) {
      super(event);
    }

    @Override
    protected void doRun() {
      assertNotSame("MuleEvent", event, originalEvent);
      assertNotNull("getCurrentEvent() is null", getCurrentEvent());
      latch.countDown();
    }
  }

}
