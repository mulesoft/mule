/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.processor.strategy.AbstractProcessingStrategy.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.WORK_QUEUE;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.WorkQueueProcessingStrategyFactory.WorkQueueProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(PROCESSING_STRATEGIES)
@Stories(WORK_QUEUE)
public class WorkQueueProcessingStrategyTestCase extends AbstractProcessingStrategyTestCase {

  public WorkQueueProcessingStrategyTestCase(Mode mode) {
    super(mode);
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
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(DefaultMuleException.class));
    expectedException.expectCause(hasMessage(equalTo(TRANSACTIONAL_ERROR_MESSAGE)));
    process(flow, testEvent());
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
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new WorkQueueProcessingStrategy(() -> new RejectingScheduler()));
    flow.setMessageProcessors(singletonList(blockingProcessor));
    flow.initialise();
    flow.start();
    expectRejected();
    process(flow, testEvent());
  }

  @Test
  @Description("If IO pool has maximum size of 1 only 1 thread is used for CPU_LIGHT processor and further requests block.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new WorkQueueProcessingStrategy(() -> new TestScheduler(1, IO)));
    internalConcurrent(true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("If IO pool has maximum size of 1 only 1 thread is used  for BLOCKING processor and further requests block.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new WorkQueueProcessingStrategy(() -> new TestScheduler(1, IO)));
    internalConcurrent(true, BLOCKING, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

}
