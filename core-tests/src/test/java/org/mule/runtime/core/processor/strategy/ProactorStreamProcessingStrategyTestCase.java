/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.PROACTOR;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.ProactorStreamProcessingStrategyFactory.ProactorStreamProcessingStrategy;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(PROCESSING_STRATEGIES)
@Stories(PROACTOR)
public class ProactorStreamProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public ProactorStreamProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                DEFAULT_BUFFER_SIZE,
                                                DEFAULT_SUBSCRIBER_COUNT,
                                                DEFAULT_WAIT_STRATEGY,
                                                () -> cpuLight,
                                                () -> blocking,
                                                () -> cpuIntensive,
                                                4);
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

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(hasMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE)));
    process(flow, testEvent());
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
    Scheduler rejectingSchedulerSpy = spy(new RejectingScheduler(blockingSchedulerSpy));

    flow = flowBuilder.get().processors(blockingProcessor)
        .processingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                             DEFAULT_BUFFER_SIZE,
                                                                                             DEFAULT_SUBSCRIBER_COUNT,
                                                                                             DEFAULT_WAIT_STRATEGY,
                                                                                             () -> cpuLight,
                                                                                             () -> rejectingSchedulerSpy,
                                                                                             () -> cpuIntensive,
                                                                                             4))
        .build();
    flow.initialise();
    flow.start();
    process(flow, testEvent());
    verify(rejectingSchedulerSpy, times(11)).submit(any(Runnable.class));
    verify(blockingSchedulerSpy, times(1)).submit(any(Runnable.class));
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
    Scheduler rejectingSchedulerSpy = spy(new RejectingScheduler(cpuIntensiveSchedulerSpy));

    flow = flowBuilder.get().processors(cpuIntensiveProcessor)
        .processingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                             DEFAULT_BUFFER_SIZE,
                                                                                             DEFAULT_SUBSCRIBER_COUNT,
                                                                                             DEFAULT_WAIT_STRATEGY,
                                                                                             () -> cpuLight,
                                                                                             () -> blocking,
                                                                                             () -> rejectingSchedulerSpy,
                                                                                             4))
        .build();
    flow.initialise();
    flow.start();
    process(flow, testEvent());
    verify(rejectingSchedulerSpy, times(11)).submit(any(Runnable.class));
    verify(cpuIntensiveSchedulerSpy, times(1)).submit(any(Runnable.class));
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
        .processingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                             DEFAULT_BUFFER_SIZE,
                                                                                             DEFAULT_SUBSCRIBER_COUNT,
                                                                                             DEFAULT_WAIT_STRATEGY,
                                                                                             () -> cpuLight,
                                                                                             () -> blocking,
                                                                                             () -> cpuIntensive,
                                                                                             1)),
                       true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(RING_BUFFER)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 1, only 1 thread is used for BLOCKING processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                             DEFAULT_BUFFER_SIZE,
                                                                                             DEFAULT_SUBSCRIBER_COUNT,
                                                                                             DEFAULT_WAIT_STRATEGY,
                                                                                             () -> cpuLight,
                                                                                             () -> blocking,
                                                                                             () -> cpuIntensive,
                                                                                             1)),
                       true, BLOCKING, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(RING_BUFFER)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If max concurrency is 2, only 2 threads are used for BLOCKING processors and further requests blocks.")
  public void singleBlockingConcurrentMaxConcurrency2() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                             DEFAULT_BUFFER_SIZE,
                                                                                             DEFAULT_SUBSCRIBER_COUNT,
                                                                                             DEFAULT_WAIT_STRATEGY,
                                                                                             () -> cpuLight,
                                                                                             () -> blocking,
                                                                                             () -> cpuIntensive,
                                                                                             2)),
                       true, BLOCKING, 2);
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(2l));
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

}
