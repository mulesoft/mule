/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assume.assumeThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.DROP;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.RejectingScheduler.REJECTION_COUNT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.CORES;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.PROACTOR;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.construct.FlowBackPressureMaxConcurrencyExceededException;
import org.mule.runtime.core.internal.construct.FlowBackPressureRequiredSchedulerBusyException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.ProactorStreamEmitterProcessingStrategyFactory.ProactorStreamEmitterProcessingStrategy;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(PROCESSING_STRATEGIES)
@Story(PROACTOR)
public class ProactorStreamEmitterProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  @Rule
  public ExpectedException expectedException = none();

  public ProactorStreamEmitterProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategyFactory createProcessingStrategyFactory() {
    return new AsyncProcessingStrategyFactory() {

      private int maxConcurrency = MAX_VALUE;

      @Override
      public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
        return createProcessingStrategy(muleContext, schedulersNamePrefix, maxConcurrency);
      }

      @Override
      public void setMaxConcurrencyEagerCheck(boolean maxConcurrencyEagerCheck) {
        // Nothing to do
      }

      @Override
      public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
      }
    };
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return createProcessingStrategy(muleContext, schedulersNamePrefix, MAX_VALUE);
  }

  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix,
                                                        int maxConcurrency) {
    return new ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                       2,
                                                       () -> cpuLight,
                                                       () -> blocking,
                                                       () -> cpuIntensive,
                                                       CORES,
                                                       maxConcurrency, true);
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when all processor are CPU_LIGHT then they are all exectured in a single "
      + " cpu light thread.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("When ProactorProcessingStrategy is configured, two concurrent requests may be processed by two different "
      + " cpu light threads. MULE-11132 is needed for true reactor behaviour.")
  public void singleCpuLightConcurrent() throws Exception {
    super.singleCpuLightConcurrent();
    assertThat(threads, hasSize(between(1, 2)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), between(1l, 2l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when all processor are CPU_LIGHT then they are all exectured in a single "
      + " cpu light thread.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, a BLOCKING message processor is scheduled on a IO thread.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, each BLOCKING message processor is scheduled on a IO thread. These may, or "
      + "may not, be the same thread.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertThat(threads, hasSize(between(1, 3)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), between(1l, 3l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, a CPU_INTENSIVE message processor is scheduled on a CPU intensive thread.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, each CPU_INTENSIVE message processor is scheduled on a CPU Intensive thread."
      + " These may, or may not, be the same thread.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertThat(threads, hasSize(between(1, 3)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), between(1l, 3l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when there is a mix of processor processing types, each processor is "
      + "scheduled on the correct scheduler.")
  public void mix() throws Exception {
    super.mix();
    assertThat(threads, hasSize(equalTo(3)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("With the ProactorProcessingStrategy, when there is a mix of processor processing types, each processor is "
      + "scheduled on the correct scheduler.")
  public void mix2() throws Exception {
    super.mix2();
    assertThat(threads, hasSize(between(3, 7)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), between(1l, 2l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), between(1l, 2l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), between(1l, 3l));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("When the ProactorProcessingStrategy is configured and a transaction is active processing fails with an error")
  public void tx() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    getInstance().bindTransaction(new TestTransaction("appName", getNotificationDispatcher(muleContext)));

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(hasMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE)));
    processFlow(testEvent());
  }

  @Override
  @Description("When the ReactorProcessingStrategy is configured and a transaction is active processing fails with an error")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertThat(threads, hasSize(between(1, 2)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), between(1l, 2l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("Concurrent stream with concurrency of 8 only uses two CPU_LIGHT threads.")
  public void concurrentStream() throws Exception {
    super.concurrentStream();
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(2l));
  }

  @Test
  @Description("If IO pool is busy OVERLOAD error is thrown")
  public void blockingRejectedExecution() throws Exception {
    Scheduler blockingSchedulerSpy = spy(blocking);
    RejectingScheduler rejectingSchedulerSpy = new RejectingScheduler(blockingSchedulerSpy);

    flow = flowBuilder.get().processors(blockingProcessor)
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    1,
                                                                                                    () -> cpuLight,
                                                                                                    () -> rejectingSchedulerSpy,
                                                                                                    () -> cpuIntensive,
                                                                                                    1,
                                                                                                    2, true))
        .build();
    flow.initialise();
    flow.start();
    rejectingSchedulerSpy.reset();

    processFlow(testEvent());

    verify(blockingSchedulerSpy, times(1)).submit(any(Callable.class));
    // Reactor dispatches different tasks to the scheduler for processing the task, so we cannot assume a 1-1 ratio between events
    // and calls to the scheduler, or that they happen all in a predictable order (threading, ya know...).
    probe(() -> {
      assertThat(rejectingSchedulerSpy.getRejections(), is(greaterThanOrEqualTo(REJECTION_COUNT)));
      return true;
    });

    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If CPU INTENSIVE pool is busy OVERLOAD error is thrown")
  public void cpuIntensiveRejectedExecution() throws Exception {
    Scheduler cpuIntensiveSchedulerSpy = spy(cpuIntensive);
    RejectingScheduler rejectingSchedulerSpy = new RejectingScheduler(cpuIntensiveSchedulerSpy);

    flow = flowBuilder.get().processors(cpuIntensiveProcessor)
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    1,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> rejectingSchedulerSpy,
                                                                                                    1,
                                                                                                    2, true))
        .build();
    flow.initialise();
    flow.start();
    rejectingSchedulerSpy.reset();

    processFlow(testEvent());

    verify(cpuIntensiveSchedulerSpy, times(1)).submit(any(Callable.class));
    // Reactor dispatches different tasks to the scheduler for processing the task, so we cannot assume a 1-1 ratio between events
    // and calls to the scheduler, or that they happen all in a predictable order (threading, ya know...).
    probe(() -> {
      assertThat(rejectingSchedulerSpy.getRejections(), is(greaterThanOrEqualTo(REJECTION_COUNT)));
      return true;
    });

    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 1, only 1 thread is used for CPU_LITE processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    1,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    CORES,
                                                                                                    1, true)),
                       true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads, hasItem(startsWith(CPU_LIGHT)));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 2, only 2 threads are used for CPU_LITE processors and further requests blocks.")
  public void singleCpuLightConcurrentMaxConcurrency2() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    2,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    CORES,
                                                                                                    2, true)),
                       true, CPU_LITE, 2);
    assertThat(threads, hasSize(2));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 1, only 1 thread is used for BLOCKING processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    assumeThat(mode, is(SOURCE));

    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    2,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    CORES,
                                                                                                    1, true)),
                       true, BLOCKING, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 2, only 2 threads are used for BLOCKING processors and further requests blocks.")
  public void singleBlockingConcurrentMaxConcurrency2() throws Exception {
    assumeThat(mode, is(SOURCE));

    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(DEFAULT_BUFFER_SIZE,
                                                                                                    2,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    1,
                                                                                                    2, true)),
                       true, BLOCKING, 2);
    assertThat(threads, hasSize(2));
    // assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(2l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("Notifications are invoked on CPU_LITE thread")
  public void asyncProcessorNotificationExecutionThreads() throws Exception {
    AtomicReference<Thread> beforeThread = new AtomicReference<>();
    AtomicReference<Thread> afterThread = new AtomicReference<>();
    testAsyncCpuLightNotificationThreads(beforeThread, afterThread);
    assertThat(beforeThread.get().getName(), startsWith(CPU_LIGHT));
    assertThat(afterThread.get().getName(), startsWith(CPU_LIGHT));
  }

  @Test
  @Description("When back-pressure strategy is 'WAIT' the source thread blocks and all requests are processed.")
  public void sourceBackPressureWait() throws Exception {
    testBackPressure(WAIT, equalTo(STREAM_ITERATIONS), equalTo(0), equalTo(STREAM_ITERATIONS));
  }

  @Test
  @Description("When back-pressure strategy is 'FAIL' some requests fail with an OVERLOAD error.")
  public void sourceBackPressureFail() throws Exception {
    testBackPressure(FAIL, lessThanOrEqualTo(STREAM_ITERATIONS), greaterThan(0), equalTo(STREAM_ITERATIONS));
  }

  @Test
  @Description("When back-pressure strategy is 'DROP' the flow rejects requests in the same way way with 'FAIL. It is the source that handles FAIL and DROP differently.")
  public void sourceBackPressureDrop() throws Exception {
    testBackPressure(DROP, lessThanOrEqualTo(STREAM_ITERATIONS), greaterThan(0), equalTo(STREAM_ITERATIONS));
  }

  @Test
  @Description("When concurrency < parallelism IO threads are still used for blocking processors to avoid cpuLight thread starvation.")
  public void concurrencyLessThanParallelism() throws Exception {
    flow = flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                                                                    1,
                                                                                                    () -> cpuLight,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    4,
                                                                                                    2, true))
        .processors(blockingProcessor)
        .build();
    flow.initialise();
    flow.start();

    processFlow(testEvent());
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If the processing type is IO_RW then processing occurs in BLOCKING thread.")
  public void singleIOWRW() throws Exception {
    super.singleIORW(() -> testEvent(), contains(IO));
    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Ignore("MULE-18521")
  public void backpressureOnInnerCpuIntensiveSchedulerBusy() throws Exception {
    assumeThat(mode, is(SOURCE));

    MultipleInvocationLatchedProcessor latchedProcessor =
        new MultipleInvocationLatchedProcessor(ReactiveProcessor.ProcessingType.CPU_INTENSIVE, 4);

    triggerableMessageSource = new TriggerableMessageSource(FAIL);

    flow = flowBuilder.get()
        .source(triggerableMessageSource)
        .processors(cpuLightProcessor, latchedProcessor)
        .maxConcurrency(6)
        .build();
    flow.initialise();
    flow.start();

    List<Future> futures = new ArrayList<>();

    try {
      // Fill the threads, the queue and an extra one to keep retrying
      for (int i = 0; i < (2 * 2) + 1; ++i) {
        futures.add(asyncExecutor.submit(() -> processFlow(newEvent())));
      }

      // Give time for the extra dispatch to get to the point where it starts retrying
      Thread.sleep(500);

      expectedException.expectCause(instanceOf(FlowBackPressureRequiredSchedulerBusyException.class));
      processFlow(newEvent());
    } finally {
      latchedProcessor.release();
      latchedProcessor.getAllLatchedLatch().await();

      futures.forEach(f -> {
        try {
          f.get(RECEIVE_TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      });
    }
  }

  @Test
  public void backpressureOnInnerCpuIntensiveSchedulerBusyRecovery() throws Exception {
    assumeThat(mode, is(SOURCE));

    MultipleInvocationLatchedProcessor latchedProcessor =
        new MultipleInvocationLatchedProcessor(ReactiveProcessor.ProcessingType.CPU_INTENSIVE, 5);

    triggerableMessageSource = new TriggerableMessageSource(FAIL);

    flow = flowBuilder.get()
        .source(triggerableMessageSource)
        .processors(cpuLightProcessor, latchedProcessor).build();
    flow.initialise();
    flow.start();

    List<Future> futures = new ArrayList<>();

    try {
      // Fill the threads, the queue and an extra one to keep retrying
      for (int i = 0; i < (2 * 2) + 1; ++i) {
        futures.add(asyncExecutor.submit(() -> processFlow(newEvent())));
      }

      // Give time for the extra dispatch to get to the point where it starts retrying
      Thread.sleep(500);

      latchedProcessor.release();

      Thread.sleep(500);
      processFlow(newEvent());
    } finally {
      futures.forEach(f -> {
        try {
          f.get(RECEIVE_TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      });
    }
  }

  @Test
  public void eagerBackpressureOnMaxConcurrencyHit() throws Exception {
    assumeThat(mode, is(SOURCE));

    MultipleInvocationLatchedProcessor latchedProcessor =
        new MultipleInvocationLatchedProcessor(CPU_LITE, 1);

    triggerableMessageSource = new TriggerableMessageSource(FAIL);

    flow = flowBuilder.get()
        .source(triggerableMessageSource)
        .processors(latchedProcessor)
        .processingStrategyFactory((muleContext, prefix) -> createProcessingStrategy(muleContext, prefix, 1))
        .build();

    flow.initialise();
    flow.start();

    List<Future> futures = new ArrayList<>();

    try {
      futures.add(asyncExecutor.submit(() -> processFlow(newEvent())));

      // Give time for the dispatch to get to the capacity check
      Thread.sleep(500);

      expectedException.expectCause(instanceOf(FlowBackPressureMaxConcurrencyExceededException.class));
      processFlow(newEvent());
    } finally {
      latchedProcessor.release();
      latchedProcessor.getAllLatchedLatch().await();

      futures.forEach(f -> {
        try {
          f.get(RECEIVE_TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      });
    }
  }

  @Test
  public void eagerBackpressureOnMaxConcurrencyHitRecovery() throws Exception {
    assumeThat(mode, is(SOURCE));

    MultipleInvocationLatchedProcessor latchedProcessor =
        new MultipleInvocationLatchedProcessor(CPU_LITE, 1);

    triggerableMessageSource = new TriggerableMessageSource(FAIL);

    flow = flowBuilder.get()
        .source(triggerableMessageSource)
        .processors(latchedProcessor)
        .processingStrategyFactory((muleContext, prefix) -> createProcessingStrategy(muleContext, prefix, 1))
        .build();

    flow.initialise();
    flow.start();

    List<Future> futures = new ArrayList<>();

    try {
      futures.add(asyncExecutor.submit(() -> processFlow(newEvent())));

      // Give time for the dispatch to get to the capacity check
      Thread.sleep(500);
      latchedProcessor.release();
      Thread.sleep(500);

      // expectedException.expectCause(instanceOf(FlowBackPressureException.class));
      processFlow(newEvent());
    } finally {
      latchedProcessor.getAllLatchedLatch().await();

      futures.forEach(f -> {
        try {
          f.get(RECEIVE_TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      });
    }
  }

  @Test
  @Ignore("As of MULE-17264, if CPU_LITE is busy, requests are bueffeed rather than rejected, as it was in 4.1.x")
  public void backpressureOnInnerCpuLightSchedulerThrowsRejectedExecution() throws Exception {
    assumeThat(mode, is(SOURCE));

    final Scheduler cpuLightBusy = new TestScheduler(2, CPU_LIGHT, true) {

      private final AtomicInteger countdown = new AtomicInteger(0);

      private void countAndThrow() {
        int v = countdown.getAndIncrement();
        if (v % 2 == 1) {
          throw new RejectedExecutionException();
        }
      }

      @Override
      public Future<?> submit(Runnable task) {
        countAndThrow();
        return super.submit(task);
      }

      @Override
      public Future<?> submit(Callable task) {
        countAndThrow();
        return super.submit(task);
      }
    };

    triggerableMessageSource = new TriggerableMessageSource(FAIL);

    flow = flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                                                                    1,
                                                                                                    () -> cpuLightBusy,
                                                                                                    () -> blocking,
                                                                                                    () -> cpuIntensive,
                                                                                                    4,
                                                                                                    2, true))
        .source(triggerableMessageSource)
        .processors(cpuLightProcessor, cpuIntensiveProcessor).build();
    flow.initialise();
    flow.start();

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(FlowBackPressureRequiredSchedulerBusyException.class));

    for (int i = 0; i < STREAM_ITERATIONS; i++) {
      processFlow(newEvent());
    }
  }

  @Test
  @Issue("MULE-17048")
  @Description("Verify that the event loop scheduler (cpu lite) is stopped before the others. Otherwise, an interrupted event may resume processing on ")
  public void schedulersStoppedInOrder() throws MuleException {
    cpuLight = spy(cpuLight);
    blocking = spy(blocking);
    cpuIntensive = spy(cpuIntensive);

    final ProcessingStrategy ps = createProcessingStrategy(muleContext, "schedulersStoppedInOrder");

    startIfNeeded(ps);
    stopIfNeeded(ps);

    final InOrder inOrder = inOrder(cpuLight, cpuIntensive, blocking);
    inOrder.verify(cpuLight).stop();
    inOrder.verify(blocking).stop();
    inOrder.verify(cpuIntensive).stop();
  }
}
