/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("Blocking Processing Strategy")
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
      + "synchronously in a caller thread and the pipeline will block caller thread until any async processors complete before continuing in the caller thread.")
  public void asyncCpuLight() throws Exception {
    super.asyncCpuLight();
    assertSynchronous(1);
  }

}
