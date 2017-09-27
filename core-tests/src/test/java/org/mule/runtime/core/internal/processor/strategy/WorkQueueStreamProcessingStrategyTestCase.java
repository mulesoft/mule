/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.DROP;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.WORK_QUEUE;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory.WorkQueueStreamProcessingStrategy;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Test;

@Feature(PROCESSING_STRATEGIES)
@Story(WORK_QUEUE)
@Ignore("MULE-13478")
public class WorkQueueStreamProcessingStrategyTestCase extends WorkQueueProcessingStrategyTestCase {

  public WorkQueueStreamProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueStreamProcessingStrategy(() -> blocking,
                                                 XS_BUFFER_SIZE,
                                                 DEFAULT_SUBSCRIBER_COUNT,
                                                 DEFAULT_WAIT_STRATEGY,
                                                 () -> blocking,
                                                 4);
  }

  @Test
  @Override
  @Description("If IO pool has maximum size of 1 only 1 thread is used for CPU_LIGHT processor and further requests block.")
  public void singleCpuLightConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new WorkQueueStreamProcessingStrategy(() -> blocking,
                                                                                              DEFAULT_BUFFER_SIZE,
                                                                                              DEFAULT_SUBSCRIBER_COUNT,
                                                                                              DEFAULT_WAIT_STRATEGY,
                                                                                              () -> blocking,
                                                                                              1)),
                       true, CPU_LITE, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

  @Test
  @Override
  @Description("If IO pool has maximum size of 1 only 1 thread is used for BLOCKING processor and further requests block.")
  public void singleBlockingConcurrentMaxConcurrency1() throws Exception {
    internalConcurrent(flowBuilder.get()
        .processingStrategyFactory((context, prefix) -> new WorkQueueStreamProcessingStrategy(() -> blocking,
                                                                                              DEFAULT_BUFFER_SIZE,
                                                                                              DEFAULT_SUBSCRIBER_COUNT,
                                                                                              DEFAULT_WAIT_STRATEGY,
                                                                                              () -> blocking,
                                                                                              1)),
                       true, BLOCKING, 1);
    assertThat(threads, hasSize(1));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(1l));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
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
