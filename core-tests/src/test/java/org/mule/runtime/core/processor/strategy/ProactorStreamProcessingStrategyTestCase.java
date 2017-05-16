/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.processor.strategy.ReactorStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.processor.strategy.ReactorStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.processor.strategy.ReactorStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.PROACTOR;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.ProactorStreamProcessingStrategyFactory.ProactorStreamProcessingStrategy;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(PROCESSING_STRATEGIES)
@Stories(PROACTOR)
public class ProactorStreamProcessingStrategyTestCase extends ProactorProcessingStrategyTestCase {

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

  @Test
  @Override
  @Description("If IO pool is busy OVERLOAD error is thrown")
  public void blockingRejectedExecution() throws Exception {
    flow.setProcessingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                                DEFAULT_BUFFER_SIZE,
                                                                                                DEFAULT_SUBSCRIBER_COUNT,
                                                                                                DEFAULT_WAIT_STRATEGY,
                                                                                                () -> cpuLight,
                                                                                                () -> new RejectingScheduler(),
                                                                                                () -> cpuIntensive,
                                                                                                4));
    flow.setMessageProcessors(singletonList(blockingProcessor));
    flow.initialise();
    flow.start();
    expectRejected();
    process(flow, testEvent());
  }

  @Test
  @Override
  @Description("If CPU INTENSIVE pool is busy OVERLOAD error is thrown")
  public void cpuIntensiveRejectedExecution() throws Exception {
    flow.setProcessingStrategyFactory((context, prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                                DEFAULT_BUFFER_SIZE,
                                                                                                DEFAULT_SUBSCRIBER_COUNT,
                                                                                                DEFAULT_WAIT_STRATEGY,
                                                                                                () -> cpuLight,
                                                                                                () -> blocking,
                                                                                                () -> new RejectingScheduler(),
                                                                                                4));
    flow.setMessageProcessors(singletonList(cpuIntensiveProcessor));
    flow.initialise();
    flow.start();
    expectRejected();
    process(flow, testEvent());
  }

  @Test
  @Override
  @Description("If max concurrency is 1, only 1 thread is used for CPU_LITE processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                       DEFAULT_BUFFER_SIZE,
                                                                                       DEFAULT_SUBSCRIBER_COUNT,
                                                                                       DEFAULT_WAIT_STRATEGY,
                                                                                       () -> cpuLight,
                                                                                       () -> blocking,
                                                                                       () -> cpuIntensive,
                                                                                       1));
    internalConcurrent(true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(RING_BUFFER)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Override
  @Description("If max concurrency is 1, only 1 thread is used for BLOCKING processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                       DEFAULT_BUFFER_SIZE,
                                                                                       DEFAULT_SUBSCRIBER_COUNT,
                                                                                       DEFAULT_WAIT_STRATEGY,
                                                                                       () -> cpuLight,
                                                                                       () -> blocking,
                                                                                       () -> cpuIntensive,
                                                                                       1));
    internalConcurrent(true, BLOCKING, 1);
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
    flow.setProcessingStrategyFactory((context,
                                       prefix) -> new ProactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                       DEFAULT_BUFFER_SIZE,
                                                                                       DEFAULT_SUBSCRIBER_COUNT,
                                                                                       DEFAULT_WAIT_STRATEGY,
                                                                                       () -> cpuLight,
                                                                                       () -> blocking,
                                                                                       () -> cpuIntensive,
                                                                                       2));
    internalConcurrent(true, BLOCKING, 2);
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(2l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

}
