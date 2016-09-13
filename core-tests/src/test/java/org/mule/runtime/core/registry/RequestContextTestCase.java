/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.createErrorMock;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

public class RequestContextTestCase extends AbstractMuleTestCase {

  private FlowConstruct flow;

  @Before
  public void before() {
    flow = mock(FlowConstruct.class);
    when(flow.getMuleContext()).thenReturn(mock(MuleContext.class));
  }

  @Test
  public void testSetExceptionPayloadAcrossThreads() throws InterruptedException {
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload("").build()).build();
    runThread(event, false);
    runThread(event, true);
  }

  @Test
  public void testFailureWithoutThreadSafeEvent() throws InterruptedException {
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload("").build()).build();
    runThread(event, false);
    runThread(event, true);
  }

  protected void runThread(Event event, boolean doTest) throws InterruptedException {
    AtomicBoolean success = new AtomicBoolean(false);
    Thread thread = new Thread(new SetExceptionPayload(event, success));
    thread.start();
    thread.join();
    if (doTest) {
      // Since events are now immutable, there should be no failures due to this!
      assertEquals(true, success.get());
    }
  }

  private class SetExceptionPayload implements Runnable {

    private Event event;
    private AtomicBoolean success;

    public SetExceptionPayload(Event event, AtomicBoolean success) {
      this.event = event;
      this.success = success;
    }

    @Override
    public void run() {
      try {
        Exception exception = new Exception();
        event = Event.builder(event)
            .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
            .error(createErrorMock(exception)).build();
        setCurrentEvent(event);
        success.set(true);
      } catch (RuntimeException e) {
        logger.error("error in thread", e);
      }
    }
  }
}
