/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static java.lang.Boolean.TRUE;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class AutoConfigurableTracingLevelConfigurationTestCase {

  private static final TracingLevel DEFAULT_LEVEL = MONITORING;
  private static final String CONF_FOLDER = "conf";
  private static final String TRACING_LEVEL_EMPTY_CONF = "tracing-level-empty.conf";
  private static final String NON_EXISTENT_CONF = "non-existent.conf";
  private static final String WRONG_LEVEL_CONF = "wrong-level.conf";

  @Test
  public void whenNoPropertyIsInTheFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestEmptyFileTracingLevelConfiguration(mock(MuleContext.class));
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(fileTracingLevelConfiguration);
    tracingLevelConfiguration.setSpanExporterConfiguration(mock(SpanExporterConfiguration.class));
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenNoFileExistsDefaultLevelIsReturned() {
    SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    when(spanExporterConfiguration.getStringValue(any(), any())).thenReturn(TRUE.toString());
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestNoFileTracingLevelConfiguration(mock(MuleContext.class));
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(fileTracingLevelConfiguration);
    tracingLevelConfiguration.setSpanExporterConfiguration(spanExporterConfiguration);
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenLevelIsWrongInFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestWrongLevelTracingLevelConfiguration(mock(MuleContext.class));
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(fileTracingLevelConfiguration);
    tracingLevelConfiguration.setSpanExporterConfiguration(mock(SpanExporterConfiguration.class));
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
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

}
