/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static java.io.File.separator;
import static java.lang.Boolean.TRUE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.ResourceNotFoundException;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class FileTracingLevelConfigurationTestCase {

  private static final String CONF_FOLDER = "conf";
  private static final String TRACING_LEVEL_CONF = "tracing-level.conf";
  private static final String TRACING_LEVEL_EMPTY_CONF = "tracing-level-empty.conf";
  private static final String TRACING_LEVEL_WITH_OVERRIDES_CONF = "tracing-level-with-overrides.conf";
  private static final String TRACING_LEVEL_WITH_WRONG_OVERRIDE_CONF = "tracing-level-with-wrong-override.conf";
  private static final String TRACING_LEVEL_WITH_DUPLICATE_OVERRIDE_CONF = "tracing-level-with-duplicate-override.conf";
  private static final String NON_EXISTENT_CONF = "non-existent.conf";
  private static final String WRONG_LEVEL_CONF = "wrong-level.conf";
  private static final String LOCATION_1 = "location1";
  private static final String LOCATION_2 = "location2";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void whenLevelIsSpecifiedInFileItIsReturned() {
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), equalTo(OVERVIEW));
  }

  @Test
  public void whenNoPropertyIsInTheFileNullIsReturned() {
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestEmptyFileTracingLevelConfiguration(mock(MuleContext.class));
    assertNull(fileTracingLevelConfiguration.getTracingLevel());
  }

  @Test
  public void whenNoFileExistsExceptionIsThrown() {
    expectedException.expect(ResourceNotFoundException.class);
    expectedException.expectMessage("Couldn't find resource: conf" + separator
        + "non-existent.conf neither on classpath or in file system");
    final SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    when(spanExporterConfiguration.getStringValue(any(), any())).thenReturn(TRUE.toString());
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestNoFileTracingLevelConfiguration(mock(MuleContext.class));
    fileTracingLevelConfiguration.getTracingLevel();
  }

  @Test
  public void whenLevelIsWrongInFileExceptionIsThrown() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("No enum constant org.mule.runtime.tracing.level.api.config.TracingLevel.LEVEL:WRONG");
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestWrongLevelTracingLevelConfiguration(mock(MuleContext.class));
    fileTracingLevelConfiguration.getTracingLevel();
  }

  @Test
  public void whenALocationOverrideIsSpecifiedInTheFileTheOverrideIsReturned() {
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelWithOverridesConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_1), equalTo(MONITORING));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_2), equalTo(TracingLevel.DEBUG));
  }

  @Test
  public void whenAWrongLocationOverrideIsSpecifiedInTheFileTheDefaultLevelIsReturned() {
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelWithWrongOverrideConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_1), equalTo(TracingLevel.OVERVIEW));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_2), equalTo(TracingLevel.DEBUG));
  }

  @Test
  public void whenALocationOverrideIsSpecifiedAndDuplicatedInTheFileTheLastOverrideIsReturned() {
    final FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelWithDuplicateOverrideConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_1), equalTo(TracingLevel.DEBUG));
    assertThat(fileTracingLevelConfiguration.getTracingLevelOverride(LOCATION_2), equalTo(TracingLevel.DEBUG));
  }

  /**
   * {@link FileTracingLevelConfiguration} used for testing a file with a defined level
   */
  private static class TestFileTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = TRACING_LEVEL_CONF;

    public TestFileTracingLevelConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing the case when the written level does not exist
   */
  private static class TestWrongLevelTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = WRONG_LEVEL_CONF;

    public TestWrongLevelTracingLevelConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing an empty file.
   */
  private static class TestEmptyFileTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = TRACING_LEVEL_EMPTY_CONF;

    public TestEmptyFileTracingLevelConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing the case where the file does not exist
   */
  private static class TestNoFileTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = NON_EXISTENT_CONF;

    public TestNoFileTracingLevelConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing the case where the file does not exist
   */
  private static class TestFileTracingLevelWithOverridesConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = TRACING_LEVEL_WITH_OVERRIDES_CONF;

    public TestFileTracingLevelWithOverridesConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing the case where the file does not exist
   */
  private static class TestFileTracingLevelWithWrongOverrideConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = TRACING_LEVEL_WITH_WRONG_OVERRIDE_CONF;

    public TestFileTracingLevelWithWrongOverrideConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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

  /**
   * {@link FileTracingLevelConfiguration} used for testing the case where the file does not exist
   */
  private static class TestFileTracingLevelWithDuplicateOverrideConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = TRACING_LEVEL_WITH_DUPLICATE_OVERRIDE_CONF;

    public TestFileTracingLevelWithDuplicateOverrideConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
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
