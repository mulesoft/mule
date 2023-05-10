/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.impl.config;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import java.util.HashMap;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class FileTracingLevelConfigurationTestCase {

  private static final String CONF_FOLDER = "conf";
  private static final String TRACING_LEVEL_CONF = "tracing-level.conf";
  private static final String TRACING_LEVEL_EMPTY_CONF = "tracing-level-empty.conf";
  private static final String TRACING_LEVEL_WITH_OVERRIDES_CONF = "tracing-level-with-overrides.conf";
  private static final String TRACING_LEVEL_WITH_ONE_WRONG_OVERRIDE_CONF = "tracing-level-with-one-wrong-override.conf";
  private static final String TRACING_LEVEL_WITH_DUPLICATE_OVERRIDE_CONF = "tracing-level-with-duplicate-override.conf";
  private static final String NON_EXISTENT_CONF = "non-existent.conf";
  private static final String WRONG_LEVEL_CONF = "wrong-level.conf";
  private static final String LOCATION_1 = "location1";
  private static final String LOCATION_2 = "location2";
  private static final TracingLevel DEFAULT_LEVEL = TracingLevel.MONITORING;

  @Test
  public void whenLevelIsSpecifiedInFileItIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_CONF);
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), equalTo(TracingLevel.OVERVIEW));
  }

  @Test
  public void whenNoPropertyIsInTheFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_EMPTY_CONF);
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenNoFileExistsDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), NON_EXISTENT_CONF);
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenLevelIsWrongInFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), WRONG_LEVEL_CONF);
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenNoFileExistsTracingLevelOverridesIsEmpty() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), NON_EXISTENT_CONF);
    assertTrue(fileTracingLevelConfiguration.getTracingLevelOverrides().isEmpty());
  }

  @Test
  public void whenNoPropertyIsInTheFileTracingLevelOverridesIsEmpty() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_EMPTY_CONF);
    assertTrue(fileTracingLevelConfiguration.getTracingLevelOverrides().isEmpty());
  }

  @Test
  public void whenOnlyTheLevelIsSpecifiedInTheFileTracingLevelOverridesIsEmpty() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_CONF);
    assertTrue(fileTracingLevelConfiguration.getTracingLevelOverrides().isEmpty());
  }

  @Test
  public void whenALocationOverrideIsSpecifiedInTheFileTheOverrideIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_WITH_OVERRIDES_CONF);
    HashMap<String, TracingLevel> tracingLevelOverrides = fileTracingLevelConfiguration.getTracingLevelOverrides();

    assertFalse(tracingLevelOverrides.isEmpty());
    assertTrue(tracingLevelOverrides.containsKey(LOCATION_1));
    assertEquals(TracingLevel.MONITORING, tracingLevelOverrides.get(LOCATION_1));
    assertTrue(tracingLevelOverrides.containsKey(LOCATION_2));
    assertEquals(TracingLevel.DEBUG, tracingLevelOverrides.get(LOCATION_2));
  }

  @Test
  public void whenAWrongLocationOverrideIsSpecifiedInTheFileTheOverrideIsNotReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_WITH_ONE_WRONG_OVERRIDE_CONF);
    HashMap<String, TracingLevel> tracingLevelOverrides = fileTracingLevelConfiguration.getTracingLevelOverrides();

    assertFalse(tracingLevelOverrides.isEmpty());
    assertFalse(tracingLevelOverrides.containsKey(LOCATION_1));
    assertTrue(tracingLevelOverrides.containsKey(LOCATION_2));
    assertEquals(TracingLevel.DEBUG, tracingLevelOverrides.get(LOCATION_2));
  }

  @Test
  public void whenALocationOverrideIsSpecifiedAndDuplicatedInTheFileTheLastOverrideIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class), TRACING_LEVEL_WITH_DUPLICATE_OVERRIDE_CONF);
    HashMap<String, TracingLevel> tracingLevelOverrides = fileTracingLevelConfiguration.getTracingLevelOverrides();

    assertFalse(tracingLevelOverrides.isEmpty());
    assertTrue(tracingLevelOverrides.containsKey(LOCATION_1));
    assertEquals(TracingLevel.DEBUG, tracingLevelOverrides.get(LOCATION_1));
    assertTrue(tracingLevelOverrides.containsKey(LOCATION_2));
    assertEquals(TracingLevel.DEBUG, tracingLevelOverrides.get(LOCATION_2));
  }

  /**
   * {@link FileTracingLevelConfiguration} used for testing a file with a defined level
   */
  private static class TestFileTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public final String testConfFileName;

    public TestFileTracingLevelConfiguration(MuleContext muleContext, String confFileName) {
      super(muleContext);
      this.testConfFileName = confFileName;
    }

    @Override
    protected String getPropertiesFileName() {
      return testConfFileName;
    }

    @Override
    protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
      return Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected String getConfFolder() {
      return CONF_FOLDER;
    }
  }
}
