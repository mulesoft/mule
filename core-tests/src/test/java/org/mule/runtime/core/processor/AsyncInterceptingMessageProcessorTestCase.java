/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;

import java.beans.ExceptionListener;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractReactiveProcessorTestCase
    implements ExceptionListener {

  protected AsyncInterceptingMessageProcessor messageProcessor;
  protected TestListener target = new TestListener();
  protected Latch latch = new Latch();
  protected Exception exceptionThrown;

  private final Processor failingProcessor = event -> {
    throw new DefaultMuleException("failure");
  };

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  public AsyncInterceptingMessageProcessorTestCase(boolean reactive) {
    super(reactive);
    setStartContext(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    messageProcessor = createAsyncInterceptingMessageProcessor(target);
  }

  @Test
  public void testProcessOneWay() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(TEST_MESSAGE)).exchangePattern(ONE_WAY).build();

    assertAsync(messageProcessor, event);
  }

  @Test
  public void testProcessRequestResponse() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(TEST_MESSAGE)).exchangePattern(REQUEST_RESPONSE).build();

    assertAsync(messageProcessor, event);
  }

  @Test
  public void testException() throws Exception {

    Flow flow = new Flow("flow", muleContext);
    initialiseObject(flow);

    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of(TEST_MESSAGE))
        .exchangePattern(ONE_WAY)
        .build();

    messageProcessor.setListener(failingProcessor);
    messageProcessor.setMuleContext(muleContext);
    messageProcessor.setFlowConstruct(flow);

    expectedException.expect(DefaultMuleException.class);
    process(messageProcessor, event);
  }

  protected void assertAsync(Processor processor, Event event)
      throws Exception {
    Event result = process(processor, event);

    latch.await(10000, MILLISECONDS);
    assertNotNull(target.sensedEvent);
    assertSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));
    assertNotSame(Thread.currentThread(), target.thread);

    assertSame(event, result);
    assertNull(exceptionThrown);
  }

  protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(Processor listener)
      throws Exception {
    AsyncInterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(() -> scheduler);
    mp.setMuleContext(muleContext);
    mp.setFlowConstruct(getTestFlow(muleContext));
    mp.setListener(listener);
    return mp;
  }

  class TestListener implements Processor {

    Event sensedEvent;
    Thread thread;

    @Override
    public Event process(Event event) throws MuleException {
      thread = Thread.currentThread();
      sensedEvent = event;
      latch.countDown();
      return event;
    }
  }

  @Override
  public void exceptionThrown(Exception e) {
    exceptionThrown = e;
  }

}
