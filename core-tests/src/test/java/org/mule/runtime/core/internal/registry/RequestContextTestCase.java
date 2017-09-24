/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class RequestContextTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(RequestContextTestCase.class);

  @Test
  public void testSetExceptionPayloadAcrossThreads() throws InterruptedException, MuleException {
    runThread(testEvent(), false);
    runThread(testEvent(), true);
  }

  @Test
  public void testFailureWithoutThreadSafeEvent() throws InterruptedException, MuleException {
    runThread(testEvent(), false);
    runThread(testEvent(), true);
  }

  protected void runThread(CoreEvent event, boolean doTest) throws InterruptedException {
    AtomicBoolean success = new AtomicBoolean(false);
    Thread thread = new Thread(new SetExceptionPayload((PrivilegedEvent) event, success));
    thread.start();
    thread.join();
    if (doTest) {
      // Since events are now immutable, there should be no failures due to this!
      assertEquals(true, success.get());
    }
  }

  private class SetExceptionPayload implements Runnable {

    private PrivilegedEvent event;
    private AtomicBoolean success;

    public SetExceptionPayload(PrivilegedEvent event, AtomicBoolean success) {
      this.event = event;
      this.success = success;
    }

    @Override
    public void run() {
      try {
        Exception exception = new Exception();
        event = PrivilegedEvent.builder(event)
            .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
            .error(createErrorMock(exception)).build();
        setCurrentEvent(event);
        success.set(true);
      } catch (RuntimeException e) {
        LOGGER.error("error in thread", e);
      }
    }
  }
}
