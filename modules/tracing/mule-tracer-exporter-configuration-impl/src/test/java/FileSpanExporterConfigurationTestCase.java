/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.System.setProperty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.config.internal.dsl.model.config.PropertyNotFoundException;
import org.mule.runtime.tracer.exporter.api.config.FileSpanExporterConfiguration;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FileSpanExporterConfigurationTestCase {

  public static final String KEY_PROPERTY_NON_SYSTEM_PROPERTY = "keyPropertyNonSystemProperty";
  public static final String VALUE_PROPERTY_NON_SYSTEM_PROPERTY = "valuePropertyNonSystemProperty";
  public static final String KEY_PROPERTY_SYSTEM_PROPERTY = "keyPropertySystemProperty";
  public static final String VALUE_PROPERTY_SYSTEM_PROPERTY = "valuePropertySystemProperty";
  public static final String NO_KEY_IN_FILE = "noKeyInFile";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void returnsTheValueForANonSystemProperty() {
    FileSpanExporterConfiguration fileSpanExporterConfiguration = new TestFileSpanExporterConfiguration();
    assertThat(fileSpanExporterConfiguration.getValue(KEY_PROPERTY_NON_SYSTEM_PROPERTY), equalTo(
                                                                                                 VALUE_PROPERTY_NON_SYSTEM_PROPERTY));
  }

  @Test
  public void returnsTheResolvedSystemProperty() {
    setProperty(KEY_PROPERTY_SYSTEM_PROPERTY, VALUE_PROPERTY_SYSTEM_PROPERTY);
    FileSpanExporterConfiguration fileSpanExporterConfiguration = new TestFileSpanExporterConfiguration();
    assertThat(fileSpanExporterConfiguration.getValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(
                                                                                             VALUE_PROPERTY_SYSTEM_PROPERTY));
  }

  @Test
  public void whenASystemPropertyCannotBeResolvedAnExceptionIsRaised() {
    expectedException.expect(PropertyNotFoundException.class);
    FileSpanExporterConfiguration fileSpanExporterConfiguration = new TestFileSpanExporterConfiguration();
    assertThat(fileSpanExporterConfiguration.getValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(
                                                                                             VALUE_PROPERTY_SYSTEM_PROPERTY));
  }

  @Test
  public void whenNoPropertyIsInTheFileNullValueIsReturned() {
    FileSpanExporterConfiguration fileSpanExporterConfiguration = new TestFileSpanExporterConfiguration();
    assertThat(fileSpanExporterConfiguration.getValue(NO_KEY_IN_FILE), is(nullValue()));
  }

  @Test
  public void whenFileIsNotFoundNoPropertyIsFound() {
    TestNoFileFoundSpanExporterConfiguration testNoFileFoundSpanExporterConfiguration =
        new TestNoFileFoundSpanExporterConfiguration();
    assertThat(testNoFileFoundSpanExporterConfiguration.getValue(KEY_PROPERTY_SYSTEM_PROPERTY), is(nullValue()));
    assertThat(testNoFileFoundSpanExporterConfiguration.getValue(KEY_PROPERTY_NON_SYSTEM_PROPERTY), is(nullValue()));
  }

  /**
   * {@link FileSpanExporterConfiguration} used for testing properties file.
   */
  private static class TestFileSpanExporterConfiguration extends FileSpanExporterConfiguration {

    public static final String TEST_CONF_FILE_NAME = "test.conf";

    @Override
    protected String getConfFolder() {
      return "";
    }

    @Override
    protected String getPropertiesFileName() {
      return TEST_CONF_FILE_NAME;
    }
  }


  /**
   * {@link FileSpanExporterConfiguration} used for testing properties file not found.
   */
  private static class TestNoFileFoundSpanExporterConfiguration extends FileSpanExporterConfiguration {

    public static final String TEST_NOT_FOUND_CONF_FILE_NAME = "test-not-found.conf";

    @Override
    protected String getPropertiesFileName() {
      return TEST_NOT_FOUND_CONF_FILE_NAME;
    }
  }
}
