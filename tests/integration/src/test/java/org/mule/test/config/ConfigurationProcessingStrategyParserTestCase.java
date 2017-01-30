/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_ATTRIBUTE;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.ASYNC_PROCESSING_STRATEGY;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.DEFAULT_PROCESSING_STRATEGY;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.SYNC_PROCESSING_STRATEGY;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacySynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ConfigurationProcessingStrategyParserTestCase extends AbstractIntegrationTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {
        {DEFAULT_PROCESSING_STRATEGY, DefaultFlowProcessingStrategyFactory.class},
        {SYNC_PROCESSING_STRATEGY, LegacySynchronousProcessingStrategyFactory.class},
        {ASYNC_PROCESSING_STRATEGY, LegacyAsynchronousProcessingStrategyFactory.class},
    });
  }

  private final Class<? extends ProcessingStrategyFactory> expectedStrategyFactoryType;

  @Rule
  public SystemProperty processingStrategyProperty;

  public ConfigurationProcessingStrategyParserTestCase(String defaultProcessingStrategy,
                                                       Class<? extends ProcessingStrategyFactory> expectedStrategyFactoryType) {
    this.expectedStrategyFactoryType = expectedStrategyFactoryType;
    processingStrategyProperty = new SystemProperty(PROCESSING_STRATEGY_ATTRIBUTE, defaultProcessingStrategy);
  }

  @Override
  protected String getConfigFile() {
    return "configuration-processing-strategy-config.xml";
  }

  @Test
  public void verifyConfigurationProcessingStrategy() throws Exception {
    assertThat(muleContext.getConfiguration().getDefaultProcessingStrategyFactory(), is(instanceOf(expectedStrategyFactoryType)));
  }
}
