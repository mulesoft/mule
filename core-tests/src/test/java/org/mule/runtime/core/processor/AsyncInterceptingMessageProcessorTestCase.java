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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor.SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.beans.ExceptionListener;

import org.junit.Test;

public class AsyncInterceptingMessageProcessorTestCase extends AbstractMuleContextTestCase
    implements ExceptionListener {

  public static final String EXPECTING_SYNCHRONOUS_EVENT_ERROR =
      "Exception expected: '" + SYNCHRONOUS_NONBLOCKING_EVENT_ERROR_MESSAGE + "'";

  protected AsyncInterceptingMessageProcessor messageProcessor;
  protected TestListener target = new TestListener();
  protected Exception exceptionThrown;
  protected Latch latch = new Latch();

  public AsyncInterceptingMessageProcessorTestCase() {
    setStartContext(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    messageProcessor = createAsyncInterceptingMessageProcessor(target);
  }

  @Test
  public void testProcessOneWay() throws Exception {
    Event event = getTestEvent(TEST_MESSAGE, ONE_WAY);

    assertAsync(messageProcessor, event);
  }

  @Test
  public void testProcessRequestResponse() throws Exception {
    Event event = getTestEvent(TEST_MESSAGE);

    try {
      messageProcessor.process(event);
      fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
    } catch (Exception e) {
    }
  }

  @Test
  public void testProcessOneWayWithTx() throws Exception {
    Event event = getTestEvent(TEST_MESSAGE, ONE_WAY);
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      messageProcessor.process(event);
      fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
    } catch (Exception e) {
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void testProcessRequestResponseWithTx() throws Exception {
    Event event = getTestEvent(TEST_MESSAGE);
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      messageProcessor.process(event);
      fail(EXPECTING_SYNCHRONOUS_EVENT_ERROR);
    } catch (Exception e) {
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void testWorkMessagingException() throws Exception {

    Flow flow = new Flow("flow", muleContext);
    LatchedExceptionListener exceptionListener = new LatchedExceptionListener();
    flow.setExceptionListener(exceptionListener);
    initialiseObject(flow);

    Event event = MuleTestUtils.getTestEvent(TEST_MESSAGE, flow, ONE_WAY, muleContext);

    Processor next = event1 -> {
      throw new MessagingException(event1, null);
    };

    messageProcessor.setListener(next);
    messageProcessor.setMuleContext(muleContext);
    messageProcessor.setFlowConstruct(flow);
    messageProcessor.process(event);

    assertTrue(exceptionListener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
  }

  @Test
  public void testWorkException() throws Exception {

    Flow flow = new Flow("flow", muleContext);
    LatchedExceptionListener exceptionListener = new LatchedExceptionListener();
    flow.setExceptionListener(exceptionListener);
    initialiseObject(flow);

    Event event = MuleTestUtils.getTestEvent(TEST_MESSAGE, flow, ONE_WAY, muleContext);

    Processor next = event1 -> {
      throw new DefaultMuleException("failure");
    };

    messageProcessor.setListener(next);
    messageProcessor.setMuleContext(muleContext);
    messageProcessor.setFlowConstruct(flow);
    messageProcessor.process(event);

    assertTrue(exceptionListener.latch.await(RECEIVE_TIMEOUT, MILLISECONDS));
  }

  protected void assertSync(Processor processor, Event event) throws MuleException {
    Event result = processor.process(event);

    assertSame(event, target.sensedEvent);
    assertSame(event, result);
    assertSame(Thread.currentThread(), target.thread);
  }

  protected void assertAsync(Processor processor, Event event)
      throws MuleException, InterruptedException {
    Event result = processor.process(event);

    latch.await(10000, MILLISECONDS);
    assertNotNull(target.sensedEvent);
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));
    assertNotSame(Thread.currentThread(), target.thread);

    assertSame(VoidMuleEvent.getInstance(), result);
    assertNull(exceptionThrown);
  }

  protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(Processor listener)
      throws Exception {
    AsyncInterceptingMessageProcessor mp = new AsyncInterceptingMessageProcessor(
                                                                                 new TestWorkManagerSource());
    mp.setMuleContext(muleContext);
    mp.setFlowConstruct(getTestFlow());
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

  class TestWorkManagerSource implements WorkManagerSource {

    @Override
    public WorkManager getWorkManager() throws MuleException {
      return muleContext.getWorkManager();
    }
  }

  private static class LatchedExceptionListener implements MessagingExceptionHandler {

    Latch latch = new Latch();

    public WildcardFilter getCommitTxFilter() {
      return null;
    }

    public WildcardFilter getRollbackTxFilter() {
      return null;
    }

    @Override
    public Event handleException(MessagingException exception, Event event) {
      latch.countDown();
      return null;
    }

  }
}
