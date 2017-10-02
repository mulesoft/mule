/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.DROP;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.ReactorProcessingStrategyFactory.ReactorProcessingStrategy;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
public class ReactorProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public ReactorProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorProcessingStrategy(() -> cpuLight);
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleCpuLight() throws Exception {
    super.singleCpuLight();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("When ReactorProcessingStrategy is configured, two concurrent requests may be processed by two different "
      + " cpu light threads.")
  public void singleCpuLightConcurrent() throws Exception {
    super.singleCpuLightConcurrent();
    assertThat(threads, hasSize(between(1, 2)));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), between(1l, 2l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If CPU LIGHT pool has maximum size of 1 only 1 thread is used and further requests block.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context,
                                    prefix) -> new ReactorProcessingStrategy(() -> new TestScheduler(1, CPU_LIGHT, true))),
                       true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleCpuLight() throws Exception {
    super.multipleCpuLight();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleBlocking() throws Exception {
    super.singleBlocking();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleBlocking() throws Exception {
    super.multipleBlocking();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void singleCpuIntensive() throws Exception {
    super.singleCpuIntensive();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void multipleCpuIntensive() throws Exception {
    super.multipleCpuIntensive();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void mix() throws Exception {
    super.mix();
    assertEverythingOnEventLoop();
  }

  @Override
  @Description("Regardless of processor type, when the ReactorProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a single cpu light thread.")
  public void mix2() throws Exception {
    super.mix2();
    assertEverythingOnEventLoop();
  }

  private void assertEverythingOnEventLoop() {
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("When the ReactorProcessingStrategy is configured and a transaction is active processing fails with an error")
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
  @Description("When the ReactorProcessingStrategy is configured any async processing will be returned to CPU_LIGHT thread. "
      + "This helps avoid deadlocks when there are reduced number of threads used by async processor.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertThat(threads.size(), between(1, 2));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), between(1l, 2l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Override
  @Description("When the ReactorProcessingStrategy is configured any async processing will be returned to CPU_LIGHT thread. "
      + "This helps avoid deadlocks when there are reduced number of threads used by async processor.")
  public void asyncCpuLightConcurrent() throws Exception {
    super.asyncCpuLightConcurrent();
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(2l));
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
  @Description("If CPU LITE pool is busy OVERLOAD error is thrown")
  public void cpuLightRejectedExecution() throws Exception {
    flow = flowBuilder.get().processors(blockingProcessor)
        .processingStrategyFactory((context, prefix) -> new ReactorProcessingStrategy(() -> new RejectingScheduler(cpuLight)))
        .build();
    flow.initialise();
    flow.start();
    expectRejected();
    processFlow(testEvent());
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

}
