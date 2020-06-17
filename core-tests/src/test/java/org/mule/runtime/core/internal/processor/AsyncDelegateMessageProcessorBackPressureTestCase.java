/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Issue("MULE-18431")
public class AsyncDelegateMessageProcessorBackPressureTestCase extends AbstractAsyncDelegateMessageProcessorTestCase {

  private FixingBackPressureSchedulerService service;

  public AsyncDelegateMessageProcessorBackPressureTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    BackPressureGeneratorProcessingStrategy strategy = new BackPressureGeneratorProcessingStrategy();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator, (ctx, n) -> strategy);
    async = createAsyncDelegateMessageProcessor(target, flow);
    service = new FixingBackPressureSchedulerService(strategy);
    async.setSchedulerService(service);
    async.start();
  }

  @Test
  public void processManyWithBackPressure() throws Exception {
    latch = new CountDownLatch(2);

    CoreEvent request = testEvent();

    CoreEvent result1 = process(async, request);
    CoreEvent result2 = process(async, request);

    // Complete parent context so we can assert event context completion based on async completion.
    ((BaseEventContext) request.getContext()).success(result1);
    ((BaseEventContext) request.getContext()).success(result2);

    Assert.assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(false));

    // Permit async processing now we have already asserted that response alone is not enough to complete event context.
    asyncEntryLatch.countDown();

    Assert.assertThat(latch.await(LOCK_TIMEOUT, MILLISECONDS), is(true));

    // Block until async completes, not just target processor.
    while (!((BaseEventContext) target.sensedEvent.getContext()).isTerminated()) {
      park100ns();
    }
    Assert.assertThat(target.sensedEvent, notNullValue());

    // Block to ensure async fully completes before testing state
    while (!((BaseEventContext) request.getContext()).isTerminated()) {
      park100ns();
    }

    Assert.assertThat(((BaseEventContext) target.sensedEvent.getContext()).isTerminated(), is(true));
    Assert.assertThat(((BaseEventContext) request.getContext()).isTerminated(), is(true));

    assertTargetEvent(request);
    assertResponse(result1);
    assertResponse(result2);
    assertThat(service.getExecutions(), is(1));
  }

  private class BackPressureGeneratorProcessingStrategy implements ProcessingStrategy {

    private boolean backPressure = true;

    @Override
    public boolean isSynchronous() {
      return true;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, event -> {
      });
    }

    @Override
    public void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {
      if (backPressure) {
        throw new FromFlowRejectedExecutionException(null);
      }
    }

    public void setBackPressure(boolean backPressure) {
      this.backPressure = backPressure;
    }

  }

  private class FixingBackPressureSchedulerService extends SimpleUnitTestSupportSchedulerService {

    private BackPressureGeneratorProcessingStrategy strategy;
    private AtomicReference<Integer> executions = new AtomicReference<>(0);

    public FixingBackPressureSchedulerService(BackPressureGeneratorProcessingStrategy strategy) {
      this.strategy = strategy;
    }

    public int getExecutions() {
      return executions.get();
    }

    @Override
    public Scheduler customScheduler(SchedulerConfig config) {
      Scheduler realScheduler = super.customScheduler(config);
      Scheduler scheduler = mock(Scheduler.class);
      doAnswer(invocationOnMock -> {
        executions.getAndUpdate(x -> x + 1);
        return realScheduler.submit(() -> {
          try {
            // So we are sure that following executions continue to go on backpressure
            // but there isn't another execution, but all the backpressure is handled
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
          strategy.setBackPressure(false);
          ((Runnable) invocationOnMock.getArgument(0)).run();
        });
      }).when(scheduler).submit(any(Runnable.class));
      return scheduler;
    }
  }
}
