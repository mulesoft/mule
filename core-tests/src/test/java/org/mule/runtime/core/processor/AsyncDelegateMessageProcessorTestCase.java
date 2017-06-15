/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.lang.Thread.currentThread;
import static java.time.Duration.ofMillis;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.DirectProcessingStrategyFactory;
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
  private Flow flow;

  @Rule
  public ExpectedException expected;

  public AsyncDelegateMessageProcessorTestCase(Mode mode) {
    super(mode);
    setStartContext(true);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    flow = builder("flow", muleContext).messageProcessors(emptyList()).build();
    flow.initialise();
    flow.start();

    messageProcessor = createAsyncDelegatMessageProcessor(target, flow);
    messageProcessor.start();
  }

  @Override
  protected void doTearDown() throws Exception {
    messageProcessor.stop();
    messageProcessor.dispose();
    flow.stop();
    flow.dispose();
    super.doTearDown();
  }

  @Test
  public void process() throws Exception {
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

    // Block to ensure async fully completes before testing state
    from(request.getContext().getCompletionPublisher()).block(ofMillis(BLOCK_TIMEOUT));

    assertCompletionDone(target.sensedEvent.getContext());
    assertCompletionDone(request.getContext());

    assertTargetEvent(request);
    assertResponse(result);
  }

  @Test
  public void processWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      Event request = testEvent();
      Event result = process(messageProcessor, request);

      // Wait until processor in async is executed to allow assertions on sensed event
      asyncEntrylatch.countDown();
      assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

      assertTargetEvent(request);
      assertResponse(result);
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void processWithBlockingProcessingStrategy() throws Exception {
    flow.dispose();
    flow = builder("flow", muleContext).messageProcessors(emptyList())
        .processingStrategyFactory(new BlockingProcessingStrategyFactory()).build();
    flow.initialise();
    flow.start();
    messageProcessor.setFlowConstruct(flow);

    process();
  }

  @Test
  public void processWithDirectProcessingStrategy() throws Exception {
    flow.dispose();
    flow = builder("flow", muleContext).messageProcessors(emptyList())
        .processingStrategyFactory(new DirectProcessingStrategyFactory()).build();
    flow.initialise();
    flow.start();
    messageProcessor.setFlowConstruct(flow);

    process();
  }

  private void assertTargetEvent(Event request) {
    // Assert that event is processed in async thread
    assertNotNull(target.sensedEvent);
    assertThat(request, not(sameInstance(target.sensedEvent)));
    assertThat(request.getCorrelationId(), equalTo(target.sensedEvent.getCorrelationId()));
    assertThat(request.getMessage(), sameInstance(target.sensedEvent.getMessage()));
    assertThat(target.thread, not(sameInstance(currentThread())));
  }

  private void assertResponse(Event result) throws MuleException {
    // Assert that response is echoed by async and no exception is thrown in flow
    assertThat(testEvent(), sameInstance(result));
    assertThat(exceptionThrown, nullValue());
  }

  protected AsyncDelegateMessageProcessor createAsyncDelegatMessageProcessor(Processor listener, FlowConstruct flowConstruct)
      throws Exception {
    AsyncDelegateMessageProcessor mp = new AsyncDelegateMessageProcessor(newChain(listener), "thread");
    mp.setFlowConstruct(flowConstruct);
    initialiseIfNeeded(mp, true, muleContext);
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
        asyncEntrylatch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      sensedEvent = event;
      thread = currentThread();
      latch.countDown();
      return event;
    }
  }

  @Override
  public void exceptionThrown(Exception e) {
    exceptionThrown = e;
  }

}
