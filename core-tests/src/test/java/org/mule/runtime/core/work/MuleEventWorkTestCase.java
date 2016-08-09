/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.work;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test case to reproduce issue described in MULE-4407 and validate fix.
 */
public class MuleEventWorkTestCase extends AbstractMuleContextTestCase {

  protected MuleEvent originalEvent;
  protected Latch latch = new Latch();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    // Create a dummy event and give it some properties
    originalEvent = getTestEvent("test");
    originalEvent.setMessage(MuleMessage.builder(originalEvent.getMessage()).addOutboundProperty("test", "val")
        .addOutboundProperty("test2", "val2").build());
    OptimizedRequestContext.unsafeSetEvent(originalEvent);
  }

  @Test
  public void testScheduleMuleEventWork() throws Exception {
    muleContext.getWorkManager().scheduleWork(new TestMuleEventWork(originalEvent));

    assertTrue("Timed out waiting for latch", latch.await(2000, TimeUnit.MILLISECONDS));

    assertSame(originalEvent, RequestContext.getEvent());
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
    assertNotSame(originalEvent, RequestContext.getEvent());
  }

  private class TestMuleEventWork extends AbstractMuleEventWork {

    public TestMuleEventWork(MuleEvent event) {
      super(event);
    }

    @Override
    protected void doRun() {
      assertNotSame("MuleEvent", event, originalEvent);
      assertNotNull("RequestContext.getEvent() is null", RequestContext.getEvent());
      latch.countDown();
    }
  }

}
