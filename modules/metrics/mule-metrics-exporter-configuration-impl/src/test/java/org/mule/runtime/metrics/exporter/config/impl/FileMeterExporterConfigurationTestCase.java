/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.config.impl;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_EXPORTER;

import static java.lang.Boolean.TRUE;
import static java.lang.System.setProperty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.config.internal.model.dsl.config.PropertyNotFoundException;
import org.mule.runtime.core.api.MuleContext;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(PROFILING)
@Story(METRICS_EXPORTER)
public class FileMeterExporterConfigurationTestCase {

  public static final String KEY_NON_SYSTEM_PROPERTY = "keyNonSystemProperty";
  public static final String KEY_PROPERTY_SYSTEM_PROPERTY = "keySystemProperty";
  public static final String VALUE_NON_SYSTEM_PROPERTY = "valueNonSystemPropertyConfDirectory";
  public static final String VALUE_SYSTEM_PROPERTY = "valueSystemProperty";
  public static final String SYSTEM_PROPERTY_VALUE = "system_property_value";
  public static final String VALUE_INTERVAL = "20";
  public static final String NO_KEY_IN_FILE = "noKeyInFile";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void returnsTheResolvedSystemProperty() {
    setProperty(VALUE_SYSTEM_PROPERTY, SYSTEM_PROPERTY_VALUE);
    FileMeterExporterConfiguration fileSpanExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    assertThat(fileSpanExporterConfiguration.getStringValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(
        SYSTEM_PROPERTY_VALUE));
  }

  @Test
  public void whenASystemPropertyCannotBeResolvedAnExceptionIsRaised() {
    expectedException.expect(PropertyNotFoundException.class);
    FileMeterExporterConfiguration fileSpanExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    assertThat(fileSpanExporterConfiguration.getStringValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(
        SYSTEM_PROPERTY_VALUE));
  }

  @Test
  public void returnsTheValueForANonSystemProperty() {
    FileMeterExporterConfiguration fileMeterExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    assertThat(fileMeterExporterConfiguration.getStringValue(KEY_NON_SYSTEM_PROPERTY), equalTo(VALUE_NON_SYSTEM_PROPERTY));
  }

  @Test
  public void whenPropertyIsNotInTheFileNullValueIsReturned() {
    FileMeterExporterConfiguration fileMeterExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    assertThat(fileMeterExporterConfiguration.getStringValue(NO_KEY_IN_FILE), is(nullValue()));
  }

  @Test
  public void whenFileIsNotFoundNoPropertyIsFound() {
    TestNoFileFoundMeterExporterConfiguration testNoFileFoundSpanExporterConfiguration =
        new TestNoFileFoundMeterExporterConfiguration(mock(MuleContext.class));
    assertThat(testNoFileFoundSpanExporterConfiguration.getStringValue(KEY_NON_SYSTEM_PROPERTY), is(nullValue()));
  }

  @Test
  public void whenValueCorrespondingToPathGetAbsoluteValue() {
    TestFileMeterExporterConfiguration testfileMeterExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    String caFileLocation =
        testfileMeterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION);
    String keyFileLocation =
        testfileMeterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION);

    assertThat(caFileLocation, is(notNullValue()));
    assertThat(keyFileLocation, is(notNullValue()));

    Path caFileLocationPath = Paths.get(caFileLocation);
    Path keyFileLocationPath = Paths.get(keyFileLocation);

    assertThat(caFileLocationPath.isAbsolute(), is(TRUE));
    assertThat(keyFileLocationPath.isAbsolute(), is(TRUE));
  }

  @Test
  public void whenValueCorrespondsToANumberGetsNumberCorrectly() {
    TestFileMeterExporterConfiguration testfileMeterExporterConfiguration =
        new TestFileMeterExporterConfiguration(mock(MuleContext.class));
    String interval =
        testfileMeterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL);

    assertThat(interval, is(equalTo(VALUE_INTERVAL)));
  }

  private static class TestFileMeterExporterConfiguration extends FileMeterExporterConfiguration {


    public static final String CONF_FOLDER = "conf";
    public static final String TEST_CONF_FILE_NAME = "test.conf";

    /**
     * {@link FileMeterExporterConfiguration} used for testing properties file.
     */
    public TestFileMeterExporterConfiguration(MuleContext muleContext) {
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
   * {@link FileMeterExporterConfiguration} used for testing properties file not found.
   */
  private static class TestNoFileFoundMeterExporterConfiguration extends FileMeterExporterConfiguration {

    public static final String CONF_FOLDER_NOT_FOUND = "conf-not-found";
    public static final String TEST_NOT_FOUND_CONF_FILE_NAME = "test-not-found.conf";

    public TestNoFileFoundMeterExporterConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_NOT_FOUND_CONF_FILE_NAME;
    }

    @Override
    protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
      return Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected String getConfFolder() {
      return CONF_FOLDER_NOT_FOUND;
    }
  }
}
