/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.beans.ExceptionListener;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncDelegateMessageProcessorTestCase extends AbstractMuleContextTestCase implements ExceptionListener {

  protected AsyncDelegateMessageProcessor messageProcessor;
  protected TestListener target = new TestListener();
  protected Exception exceptionThrown;
  protected Latch latch = new Latch();

  public AsyncDelegateMessageProcessorTestCase() {
    setStartContext(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    messageProcessor = createAsyncDelegatMessageProcessor(target);
    messageProcessor.initialise();
    messageProcessor.start();
  }

  @Test
  public void testProcessOneWay() throws Exception {
    MuleEvent event = getTestEvent(TEST_MESSAGE);

    MuleEvent result = messageProcessor.process(event);

    assertThat(latch.await(10000, TimeUnit.MILLISECONDS), is(true));
    assertThat(target.sensedEvent, notNullValue());
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));

    assertSame(VoidMuleEvent.getInstance(), result);
    assertNull(exceptionThrown);
    assertNotSame(Thread.currentThread(), target.thread);

    messageProcessor.stop();
    messageProcessor.dispose();
  }

  @Test
  public void testProcessRequestResponse() throws Exception {
    MuleEvent event = getTestEvent(TEST_MESSAGE);

    MuleEvent result = messageProcessor.process(event);

    latch.await(10000, TimeUnit.MILLISECONDS);
    assertNotNull(target.sensedEvent);
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));

    assertSame(VoidMuleEvent.getInstance(), result);
    assertNull(exceptionThrown);
    assertNotSame(Thread.currentThread(), target.thread);

    messageProcessor.stop();
    messageProcessor.dispose();
  }

  @Test
  public void testProcessOneWayWithTx() throws Exception {
    MuleEvent event = getTestEvent(TEST_MESSAGE);
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      messageProcessor.process(event);
      fail("Exception expected");
    } catch (Exception e) {
      assertTrue(e instanceof MessagingException);
      assertNull(target.sensedEvent);
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void testProcessRequestResponseWithTx() throws Exception {
    MuleEvent event = getTestEvent(TEST_MESSAGE);
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      assertAsync(messageProcessor, event);
      fail("Exception expected");
    } catch (Exception e) {
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  protected void assertSync(MessageProcessor processor, MuleEvent event) throws MuleException {
    MuleEvent result = processor.process(event);

    assertSame(event, target.sensedEvent);
    assertSame(event, result);
  }

  protected void assertAsync(MessageProcessor processor, MuleEvent event) throws MuleException, InterruptedException {
    MuleEvent result = processor.process(event);

    latch.await(10000, TimeUnit.MILLISECONDS);
    assertNotNull(target.sensedEvent);
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));

    assertNull(result);
    assertNull(exceptionThrown);
  }

  protected AsyncDelegateMessageProcessor createAsyncDelegatMessageProcessor(MessageProcessor listener) throws Exception {
    AsyncDelegateMessageProcessor mp =
        new AsyncDelegateMessageProcessor(listener, new AsynchronousProcessingStrategy(), "thread");
    mp.setMuleContext(muleContext);
    mp.setFlowConstruct(new Flow("flow", muleContext));
    mp.initialise();
    return mp;
  }

  class TestListener implements MessageProcessor {

    MuleEvent sensedEvent;
    Thread thread;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      sensedEvent = event;
      thread = Thread.currentThread();
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

}
