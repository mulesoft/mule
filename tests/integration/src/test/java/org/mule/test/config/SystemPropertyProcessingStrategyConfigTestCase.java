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
import static org.mule.runtime.core.api.config.MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.NON_BLOCKING_PROCESSING_STRATEGY;
import static org.mule.runtime.core.util.ProcessingStrategyUtils.SYNC_PROCESSING_STRATEGY;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyNonBlockingProcessingStrategyFactory;
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
public class SystemPropertyProcessingStrategyConfigTestCase extends AbstractIntegrationTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {
        {"Container level system property", new String[] {}, LegacySynchronousProcessingStrategyFactory.class},
        {"Configuration overrides system property", new String[] {"configuration-processing-strategy-config.xml"},
            LegacyNonBlockingProcessingStrategyFactory.class}
    });
  }

  private final String[] configFiles;
  private Class<? extends ProcessingStrategyFactory> expectedStrategyFactoryType;

  @Rule
  public SystemProperty globalProcessingStrategy =
      new SystemProperty(MULE_DEFAULT_PROCESSING_STRATEGY, SYNC_PROCESSING_STRATEGY);

  @Rule
  public SystemProperty localProcessingStrategy =
      new SystemProperty(PROCESSING_STRATEGY_ATTRIBUTE, NON_BLOCKING_PROCESSING_STRATEGY);

  public SystemPropertyProcessingStrategyConfigTestCase(String name, String[] configFiles,
                                                        Class<? extends ProcessingStrategyFactory> expectedStrategyFactoryType) {
    this.configFiles = configFiles;
    this.expectedStrategyFactoryType = expectedStrategyFactoryType;
  }

  @Override
  protected String[] getConfigFiles() {
    return configFiles;
  }

  @Test
  public void assertDefaultProcessingStrategy() throws Exception {
    assertThat(muleContext.getConfiguration().getDefaultProcessingStrategyFactory(), is(instanceOf(expectedStrategyFactoryType)));
  }
}
