/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Arrays.asList;
import static org.mule.runtime.core.processor.strategy.LegacySynchronousProcessingStrategyFactory.LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.BLOCKING;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.Collection;

import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("Legacy Synchronous Processing Strategy")
public class LegacySynchronousProcessingStrategyTestCase extends SynchronousProcessingStrategyTestCase {

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {{BLOCKING}});
  }

  public LegacySynchronousProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }

}
