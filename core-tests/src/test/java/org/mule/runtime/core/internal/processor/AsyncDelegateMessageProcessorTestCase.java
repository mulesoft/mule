/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.beans.ExceptionListener;
import java.util.Map;

public class AsyncDelegateMessageProcessorTestCase extends AbstractReactiveProcessorTestCase implements ExceptionListener {

  private AsyncDelegateMessageProcessor messageProcessor;
  protected TestListener target = new TestListener();
  private Exception exceptionThrown;
  protected Latch latch = new Latch();
  private Latch asyncEntryLatch = new Latch();
  private Flow flow;

  @Rule
  public ExpectedException expected;

  public AsyncDelegateMessageProcessorTestCase(Mode mode) {
    super(mode);
    setStartContext(true);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);

    messageProcessor = createAsyncDelegateMessageProcessor(target, flow);
    messageProcessor.start();
  }

  @Override
  protected void doTearDown() throws Exception {
    messageProcessor.stop();
    messageProcessor.dispose();
    super.doTearDown();
  }

  @Test
  public void process() throws Exception {
    CoreEvent request = testEvent();

    CoreEvent result = process(messageProcessor, request);

    // Complete parent context so we can assert event context completion based on async completion.
    ((BaseEventContext) request.getContext()).success(result);

    assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(false));

    // Permit async processing now we have already asserted that response alone is not enough to complete event context.
    asyncEntryLatch.countDown();

    assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

    // Block until async completes, not just target processor.
    while (!((BaseEventContext) target.sensedEvent.getContext()).isTerminated()) {
      park100ns();
    }
    assertThat(target.sensedEvent, notNullValue());

    // Block to ensure async fully completes before testing state
    while (!((BaseEventContext) request.getContext()).isTerminated()) {
      park100ns();
    }

    assertThat(((BaseEventContext) target.sensedEvent.getContext()).isTerminated(), is(true));
    assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(true));

    assertTargetEvent(request);
    assertResponse(result);
  }

  private void park100ns() {
    parkNanos(100);
  }

  @Test
  public void processWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      CoreEvent request = testEvent();
      CoreEvent result = process(messageProcessor, request);

      // Wait until processor in async is executed to allow assertions on sensed event
      asyncEntryLatch.countDown();
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
    flow = builder("flow", muleContext).processingStrategyFactory(new BlockingProcessingStrategyFactory()).build();
    flow.initialise();
    flow.start();

    process();
  }

  @Test
  public void processWithDirectProcessingStrategy() throws Exception {
    flow.dispose();
    flow = builder("flow", muleContext).processingStrategyFactory(new DirectProcessingStrategyFactory()).build();
    flow.initialise();
    flow.start();

    process();
  }

  private void assertTargetEvent(CoreEvent request) {
    // Assert that event is processed in async thread
    assertNotNull(target.sensedEvent);
    assertThat(request, not(sameInstance(target.sensedEvent)));
    assertThat(request.getCorrelationId(), equalTo(target.sensedEvent.getCorrelationId()));
    assertThat(request.getMessage(), sameInstance(target.sensedEvent.getMessage()));
    assertThat(target.thread, not(sameInstance(currentThread())));
  }

  private void assertResponse(CoreEvent result) throws MuleException {
    // Assert that response is echoed by async and no exception is thrown in flow
    assertThat(testEvent(), sameInstance(result));
    assertThat(exceptionThrown, nullValue());
  }

  private AsyncDelegateMessageProcessor createAsyncDelegateMessageProcessor(Processor listener, FlowConstruct flowConstruct)
      throws Exception {
    AsyncDelegateMessageProcessor mp =
        new AsyncDelegateMessageProcessor(newChain(of(flowConstruct.getProcessingStrategy()), listener), "thread");
    mp.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(mp, true, muleContext);
    return mp;
  }

  class TestListener implements Processor {

    CoreEvent sensedEvent;
    Thread thread;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        asyncEntryLatch.await();
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
