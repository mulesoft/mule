/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.processor.strategy.SynchronousStreamProcessingStrategyFactory.SYNCHRONOUS_STREAM_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("Synchronous Processing Strategy")
public class SynchronousStreamProcessingStrategyTestCase extends SynchronousProcessingStrategyTestCase {

  public SynchronousStreamProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return SYNCHRONOUS_STREAM_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  @Description("Regardless of processor type, when the SynchronousProcessingStrategy is configured, the pipeline is executed "
      + "synchronously in a caller thread.")
  public void singleCpuLightConcurrent() throws Exception {
    super.internalSingleCpuLightConcurrent(true);
    assertSynchronous(1);
  }

}
