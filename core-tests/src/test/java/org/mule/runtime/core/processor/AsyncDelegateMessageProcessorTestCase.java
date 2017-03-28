/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.beans.ExceptionListener;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AsyncDelegateMessageProcessorTestCase extends AbstractReactiveProcessorTestCase implements ExceptionListener {

  protected AsyncDelegateMessageProcessor messageProcessor;
  protected TestListener target = new TestListener();
  protected Exception exceptionThrown;
  protected Latch latch = new Latch();
  protected Latch asyncEntrylatch = new Latch();

  @Rule
  public ExpectedException expected;

  public AsyncDelegateMessageProcessorTestCase(Mode mode) {
    super(mode);
    setStartContext(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    messageProcessor = createAsyncDelegatMessageProcessor(target);
    messageProcessor.initialise();
    messageProcessor.start();
  }

  @Override
  protected void doTearDown() throws Exception {
    messageProcessor.stop();
    messageProcessor.dispose();
    super.doTearDown();
  }

  @Test
  public void testProcessOneWay() throws Exception {
    Event request = testEvent();

    Event result = process(messageProcessor, request);

    // Complete parent context so we can assert event context completion based on async completion.
    request.getContext().success(result);

    assertCompletionNotDone(request.getContext());

    // Permit async processing now we have already asserted that response alone is not enough to complete event context.
    asyncEntrylatch.countDown();

    assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

    // Block until async completes, not just target processor.
    from(target.sensedEvent.getContext().getCompletionPublisher()).block(ofMillis(BLOCK_TIMEOUT));
    assertThat(target.sensedEvent, notNullValue());
    assertCompletionDone(target.sensedEvent.getContext());
    assertCompletionDone(request.getContext());

    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertThat(testEvent(), not(sameInstance(target.sensedEvent)));
    assertThat(testEvent().getMessageAsString(muleContext), equalTo(target.sensedEvent.getMessageAsString(muleContext)));

    assertThat(testEvent(), sameInstance(result));
    assertThat(exceptionThrown, nullValue());
    assertThat(target.thread, not(sameInstance(Thread.currentThread())));

    messageProcessor.stop();
    messageProcessor.dispose();
  }

  @Test
  public void testProcessRequestResponse() throws Exception {
    Event request = testEvent();
    Event result = process(messageProcessor, testEvent());

    // Complete parent context so we can assert event context completion based on async completion.
    request.getContext().success(result);

    assertCompletionNotDone(request.getContext());

    // Permit async processing now we have already asserted that response alone is not enough to complete event context.
    asyncEntrylatch.countDown();

    assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

    // Block until async completes, not just target processor.
    from(target.sensedEvent.getContext().getCompletionPublisher()).block(ofMillis(BLOCK_TIMEOUT));
    assertThat(target.sensedEvent, notNullValue());
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(request, target.sensedEvent);
    assertEquals(testEvent().getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));
    assertCompletionDone(target.sensedEvent.getContext());
    assertCompletionDone(request.getContext());

    assertSame(testEvent(), result);
    assertNull(exceptionThrown);
    assertNotSame(Thread.currentThread(), target.thread);

    messageProcessor.stop();
    messageProcessor.dispose();
  }

  @Test
  public void testProcessOneWayWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      process(messageProcessor, testEvent());
      fail("Exception expected");
    } catch (Exception e) {
      assertThat(e, instanceOf(RoutingException.class));
      assertNull(target.sensedEvent);
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void testProcessRequestResponseWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      assertAsync(messageProcessor, process(messageProcessor, testEvent()));
      fail("Exception expected");
    } catch (Exception e) {
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  protected void assertAsync(Processor processor, Event event) throws MuleException, InterruptedException {
    Event result = processor.process(event);

    latch.await(10000, MILLISECONDS);
    assertNotNull(target.sensedEvent);
    // Event is not the same because it gets copied in
    // AbstractMuleEventWork#run()
    assertNotSame(event, target.sensedEvent);
    assertEquals(event.getMessageAsString(muleContext), target.sensedEvent.getMessageAsString(muleContext));

    assertNull(result);
    assertNull(exceptionThrown);
  }

  protected AsyncDelegateMessageProcessor createAsyncDelegatMessageProcessor(Processor listener) throws Exception {
    AsyncDelegateMessageProcessor mp =
        new AsyncDelegateMessageProcessor(MessageProcessors.newChain(listener), new LegacyAsynchronousProcessingStrategyFactory(),
                                          "thread");
    mp.setMuleContext(muleContext);
    final Flow flowConstruct = builder("flow", muleContext).build();
    flowConstruct.initialise();
    mp.setFlowConstruct(flowConstruct);
    mp.initialise();
    return mp;
  }

  private void assertCompletionDone(EventContext parent) {
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  private void assertCompletionNotDone(EventContext child1) {
    assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(false));
  }

  class TestListener implements Processor {

    Event sensedEvent;
    Thread thread;

    @Override
    public Event process(Event event) throws MuleException {
      try {
        asyncEntrylatch.await(LOCK_TIMEOUT, MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
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

}
