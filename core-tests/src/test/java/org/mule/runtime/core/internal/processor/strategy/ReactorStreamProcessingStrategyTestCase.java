/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.DROP;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.ReactorStreamProcessingStrategyFactory.ReactorStreamProcessingStrategy;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Test;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
@Ignore("MULE-13478")
public class ReactorStreamProcessingStrategyTestCase extends ReactorProcessingStrategyTestCase {

  public ReactorStreamProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorStreamProcessingStrategy(() -> ringBuffer,
                                               XS_BUFFER_SIZE,
                                               DEFAULT_SUBSCRIBER_COUNT,
                                               DEFAULT_WAIT_STRATEGY,
                                               () -> cpuLight,
                                               MAX_VALUE);
  }

  @Test
  @Override
  @Description("If max concurrency is 1, only 1 thread is used for BLOCKING processors and further requests blocks. When " +
      "maxConcurrency < subscribers processing is done on ring-buffer thread.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ReactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                            DEFAULT_BUFFER_SIZE,
                                                                                            DEFAULT_SUBSCRIBER_COUNT,
                                                                                            DEFAULT_WAIT_STRATEGY,
                                                                                            () -> cpuLight,
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
  @Description("If max concurrency is 2, only 2 threads are used for BLOCKING processors and further requests blocks.")
  public void singleCpuLightConcurrentMaxConcurrency2() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new ReactorStreamProcessingStrategy(() -> ringBuffer,
                                                                                            DEFAULT_BUFFER_SIZE,
                                                                                            DEFAULT_SUBSCRIBER_COUNT,
                                                                                            DEFAULT_WAIT_STRATEGY,
                                                                                            () -> cpuLight,
                                                                                            2)),
                       true, CPU_LITE, 2);
    assertThat(threads, hasSize(2));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(2l));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Description("When back-pressure strategy is 'WAIT' the source thread blocks and all requests are processed.")
  public void sourceBackPressureWait() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(WAIT, equalTo(STREAM_ITERATIONS), equalTo(0), equalTo(STREAM_ITERATIONS));
    }
  }

  @Test
  @Description("When back-pressure strategy is 'FAIL' some requests fail with an OVERLOAD error.")
  public void sourceBackPressureFail() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(FAIL, lessThan(STREAM_ITERATIONS), greaterThan(0), equalTo(STREAM_ITERATIONS));
    }
  }

  @Test
  @Description("When back-pressure strategy is 'DROP' some requests fail with and are dropped.")
  public void sourceBackPressureDrop() throws Exception {
    if (mode.equals(SOURCE)) {
      testBackPressure(DROP, lessThan(STREAM_ITERATIONS), equalTo(0), lessThan(STREAM_ITERATIONS));
    }
  }

}
