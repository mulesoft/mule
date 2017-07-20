/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.BLOCKING;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROCESSING_STRATEGIES)
@Story(BLOCKING)
public class BlockingProcessingStrategyTestCase extends DirectProcessingStrategyTestCase {

  public BlockingProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return BLOCKING_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  @Description("Regardless of processor type, when the BlockingProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread and the pipeline will block caller thread until any async processors complete " +
      "before continuing in the caller thread.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
  }

  @Override
  protected void assertAsyncCpuLight() {
    assertSynchronous(1);
  }

  @Override
  @Description("Regardless of processor type, when the BlockingProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread and the pipeline will block caller thread until any async processors complete " +
      "before continuing in the caller thread.")
  public void asyncCpuLightConcurrent() throws Exception {
    internalConcurrent(flowBuilder.get(), false, CPU_LITE, 1, asyncProcessor);
    assertSynchronous(2);
  }

}
