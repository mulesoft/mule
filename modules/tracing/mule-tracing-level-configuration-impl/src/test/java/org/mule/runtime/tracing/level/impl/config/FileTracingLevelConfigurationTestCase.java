/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.impl.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import org.junit.Test;

public class FileTracingLevelConfigurationTestCase {

  public static final String CONF_FOLDER = "conf";
  private static final TracingLevel DEFAULT_LEVEL = TracingLevel.MONITORING;

  @Test
  public void whenLevelIsSpecifiedInFileItIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestFileTracingLevelConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), equalTo(
                                                                        TracingLevel.OVERVIEW));
  }

  @Test
  public void whenNoPropertyIsInTheFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestEmptyFileTracingLevelConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenNoFileExistsDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestNoFileTracingLevelConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenLevelIsWrongInFileDefaultLevelIsReturned() {
    FileTracingLevelConfiguration fileTracingLevelConfiguration =
        new TestWrongLevelTracingLevelConfiguration(mock(MuleContext.class));
    assertThat(fileTracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  /**
   * {@link FileTracingLevelConfiguration} used for testing a file with a defined level
   */
  private static class TestFileTracingLevelConfiguration extends FileTracingLevelConfiguration {

    public static final String TEST_CONF_FILE_NAME = "tracing-level.conf";

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

    public static final String TEST_CONF_FILE_NAME = "wrong-level.conf";

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

    public static final String TEST_CONF_FILE_NAME = "tracing-level-empty.conf";

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

    public static final String TEST_CONF_FILE_NAME = "non-existent.conf";

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
