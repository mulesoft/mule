/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.DROP;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.WORK_QUEUE;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.WorkQueueProcessingStrategyFactory.WorkQueueProcessingStrategy;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(PROCESSING_STRATEGIES)
@Story(WORK_QUEUE)
public class WorkQueueProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public WorkQueueProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Before
  public void before() throws RegistrationException {
    super.before();
    // This processing strategy depends on blocking scheduler not rejecting work from caller thread in order to apply
    // back-pressure.
    blocking = new TestScheduler(4, IO, false);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueProcessingStrategy(() -> blocking);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void singleCpuLightConcurrent() throws Exception {
    super.singleCpuLightConcurrent();
    assertThat(threads.size(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(2)));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), allOf(
                                                                                   greaterThanOrEqualTo(1l),
                                                                                   lessThanOrEqualTo(2l)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void mix() throws Exception {
    super.mix();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("Regardless of processor type, when the WorkQueueProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single IO thead.")
  public void mix2() throws Exception {
    super.mix2();
    assertSynchronousIOScheduler(1);
  }

  @Override
  @Description("When the WorkQueueProcessingStrategy is configured and a transaction is active processing fails with an error")
  public void tx() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(hasMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE)));
    processFlow(testEvent());
  }

  @Override
  @Description("When the WorkQueueProcessingStrategy is configured any async processing will be returned to IO thread. "
      + "This helps avoid deadlocks when there are reduced number of threads used by async processor.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertThat(threads.size(), between(1, 2));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), between(1l, 2l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("When the WorkQueueProcessingStrategy is configured any async processing will be returned to IO thread. "
      + "This helps avoid deadlocks when there are reduced number of threads used by async processor.")
  public void asyncCpuLightConcurrent() throws Exception {
    super.asyncCpuLightConcurrent();
    assertThat(threads.size(), between(2, 4));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), between(2l, 4l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  private void assertSynchronousIOScheduler(int concurrency) {
    assertThat(threads.size(), equalTo(concurrency));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo((long) concurrency));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("Concurrent stream with concurrency of 8 only uses four IO threads.")
  public void concurrentStream() throws Exception {
    super.concurrentStream();
    assertThat(threads, hasSize(4));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(4l));
  }

  @Test
  @Description("If IO pool is busy OVERLOAD error is thrown")
  public void rejectedExecution() throws Exception {
    flow = flowBuilder.get().processors(blockingProcessor)
        .processingStrategyFactory((context, prefix) -> new WorkQueueProcessingStrategy(() -> new RejectingScheduler(blocking)))
        .build();
    flow.initialise();
    flow.start();
    expectRejected();
    processFlow(testEvent());
  }

  @Test
  @Description("If IO pool has maximum size of 1 only 1 thread is used for CPU_LIGHT processor and further requests block.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new WorkQueueProcessingStrategy(() -> new TestScheduler(1, IO, true))),
                       true,
                       CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If IO pool has maximum size of 1 only 1 thread is used  for BLOCKING processor and further requests block.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new WorkQueueProcessingStrategy(() -> new TestScheduler(1, IO, true))),
                       true,
                       BLOCKING, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("Notifications are invoked on IO thread")
  public void asyncProcessorNotificationExecutionThreads() throws Exception {
    AtomicReference<Thread> beforeThread = new AtomicReference<>();
    AtomicReference<Thread> afterThread = new AtomicReference<>();
    testAsyncCpuLightNotificationThreads(beforeThread, afterThread);
    assertThat(beforeThread.get().getName(), startsWith(IO));
    assertThat(afterThread.get().getName(), startsWith(IO));
  }

  @Test
  @Description("Regardless of back-pressure strategy this processing strategy blocks and processes all events")
  public void sourceBackPressureWait() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(WAIT, equalTo(STREAM_ITERATIONS), equalTo(0), equalTo(STREAM_ITERATIONS));
    }
  }

  @Test
  @Description("Regardless of back-pressure strategy this processing strategy blocks and processes all events")
  public void sourceBackPressureFail() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(FAIL, equalTo(STREAM_ITERATIONS), equalTo(0), equalTo(STREAM_ITERATIONS));
    }
  }

  @Test
  @Description("Regardless of back-pressure strategy this processing strategy blocks and processes all events")
  public void sourceBackPressureDrop() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(DROP, equalTo(STREAM_ITERATIONS), equalTo(0), equalTo(STREAM_ITERATIONS));
    }
  }

  @Test
  @Description("If IO pool is busy OVERLOAD error is thrown")
  public void blockingRejectedExecution() throws Exception {
    Scheduler blockingSchedulerSpy = spy(blocking);
    Scheduler rejectingSchedulerSpy = spy(new RejectingScheduler(blockingSchedulerSpy));

    flow = flowBuilder.get().processors(blockingProcessor)
        .processingStrategyFactory((context, prefix) -> new WorkQueueProcessingStrategy(() -> rejectingSchedulerSpy))
        .build();
    flow.initialise();
    flow.start();
    expectRejected();
    processFlow(testEvent());
  }

}
