/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.CORES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.PROACTOR;

import org.junit.Test;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROCESSING_STRATEGIES)
@Story(PROACTOR)
public class ReactorStreamProcessingStrategyFactoryTestCase extends AbstractMuleContextTestCase {

  private ReactorStreamProcessingStrategyFactory processingStrategy = new ReactorStreamProcessingStrategyFactory();

  @Test
  @Description("Number of CPU Light threads is limited to number of cores when max concurrency is MAX_VALUE.")
  public void cpuLightCountUnlimitedConcurrency() {
    assertThat(processingStrategy.resolveParallelism(), equalTo(CORES));
  }

  @Test
  @Description("Number of CPU Light threads is limited by max concurrency.")
  public void cpuLightCountMaxConcurrency2() {
    processingStrategy.setMaxConcurrency(2);
    assertThat(processingStrategy.resolveParallelism(), equalTo(2));

  }

  @Test
  @Description("Number of CPU Light threads is limited to number of cores when max concurrency > cores")
  public void cpuLightCountMaxConcurrency21() {
    processingStrategy.setMaxConcurrency(21);
    assertThat(processingStrategy.resolveParallelism(), equalTo(CORES));

  }

  @Test
  @Description("Number of CPU Light threads used considers the number of subscribers")
  public void cpuLightCountMaxConcurrency2SubscriberCount2() {
    processingStrategy.setMaxConcurrency(2);
    processingStrategy.setSubscriberCount(2);
    assertThat(processingStrategy.resolveParallelism(), equalTo(1));
  }

}
