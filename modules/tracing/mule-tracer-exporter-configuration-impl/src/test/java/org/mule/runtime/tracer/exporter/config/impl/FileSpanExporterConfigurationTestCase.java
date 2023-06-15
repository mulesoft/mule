/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.config.impl;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.impl.watcher.TracingConfigurationFileWatcherProperties.DEFAULT_DELAY_PROPERTY;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.io.File.createTempFile;
import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.config.internal.model.dsl.config.PropertyNotFoundException;
import org.mule.runtime.core.api.MuleContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunWith(Parameterized.class)
public class FileSpanExporterConfigurationTestCase {

  public static final String CONF_FOLDER = "conf";
  public static final String CONF_FOLDER_NOT_FOUND = "conf-not-found";

  public static final String TEST_NOT_FOUND_CONF_FILE_NAME = "test-not-found.conf";
  public static final String TEST_CONF_FILE_NAME = "test.conf";
  public static final String SYSTEM_PROPERTY_VALUE = "system_property_value";
  private static final long TIMEOUT_MILLIS = 10000l;
  private final boolean enableConfigInFile;
  private final String valueNonSystemProperty;
  private final String valueSystemProperty;

  @Parameterized.Parameters(name = "enableConfigInFile: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{true, "valueNonSystemProperty", "valueSystemProperty"},
        {false, "valueNonSystemPropertyConfDirectory", "valueSystemPropertyConfDirectory"}});
  }

  @Before
  public void before() {
    setProperty(ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY, Boolean.toString(enableConfigInFile));
    setProperty(DEFAULT_DELAY_PROPERTY, "1000");
  }

  @After
  public void after() {
    clearProperty(KEY_PROPERTY_SYSTEM_PROPERTY);
    clearProperty(ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY);
    clearProperty(DEFAULT_DELAY_PROPERTY);
  }

  public FileSpanExporterConfigurationTestCase(boolean enableConfigInFile, String valuePropertyNonSystemPropertyConfDirectory,
                                               String valuePropertySystemProperty) {
    this.enableConfigInFile = enableConfigInFile;
    this.valueNonSystemProperty = valuePropertyNonSystemPropertyConfDirectory;
    this.valueSystemProperty = valuePropertySystemProperty;
  }

  public static final String KEY_PROPERTY_NON_SYSTEM_PROPERTY = "keyNonSystemProperty";
  public static final String KEY_PROPERTY_SYSTEM_PROPERTY = "keySystemProperty";
  public static final String NO_KEY_IN_FILE = "noKeyInFile";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void returnsTheValueForANonSystemProperty() {
    FileSpanExporterConfiguration fileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER, TEST_CONF_FILE_NAME);
    assertThat(fileSpanExporterConfiguration.getStringValue(KEY_PROPERTY_NON_SYSTEM_PROPERTY), equalTo(
                                                                                                       valueNonSystemProperty));
  }

  @Test
  public void returnsTheResolvedSystemProperty() {
    setProperty(valueSystemProperty, SYSTEM_PROPERTY_VALUE);
    FileSpanExporterConfiguration fileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER, TEST_CONF_FILE_NAME);
    assertThat(fileSpanExporterConfiguration.getStringValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(
                                                                                                   SYSTEM_PROPERTY_VALUE));
  }

  @Test
  public void whenASystemPropertyCannotBeResolvedAnExceptionIsRaised() {
    expectedException.expect(PropertyNotFoundException.class);
    FileSpanExporterConfiguration fileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER, TEST_CONF_FILE_NAME);
    assertThat(fileSpanExporterConfiguration.getStringValue(KEY_PROPERTY_SYSTEM_PROPERTY), equalTo(valueSystemProperty));
  }

  @Test
  public void whenNoPropertyIsInTheFileNullValueIsReturned() {
    FileSpanExporterConfiguration fileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER, TEST_CONF_FILE_NAME);
    assertThat(fileSpanExporterConfiguration.getStringValue(NO_KEY_IN_FILE), is(nullValue()));
  }

  @Test
  public void whenFileIsNotFoundNoPropertyIsFound() {
    FileSpanExporterConfiguration testNoFileFoundSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER_NOT_FOUND, TEST_NOT_FOUND_CONF_FILE_NAME);
    assertThat(testNoFileFoundSpanExporterConfiguration.getStringValue(KEY_PROPERTY_SYSTEM_PROPERTY), is(nullValue()));
    assertThat(testNoFileFoundSpanExporterConfiguration.getStringValue(KEY_PROPERTY_NON_SYSTEM_PROPERTY), is(nullValue()));
  }

  @Test
  public void whenValueCorrespondingToPathGetAbsoluteValue() {
    TestFileSpanExporterConfiguration testFileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), CONF_FOLDER, TEST_CONF_FILE_NAME);
    String caFileLocation = testFileSpanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION);
    String keyFileLocation = testFileSpanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);

    assertThat(caFileLocation, is(notNullValue()));
    assertThat(keyFileLocation, is(notNullValue()));

    Path caFileLocationPath = Paths.get(caFileLocation);
    Path keyFileLocationPath = Paths.get(keyFileLocation);

    assertThat(caFileLocationPath.isAbsolute(), is(TRUE));
    assertThat(keyFileLocationPath.isAbsolute(), is(TRUE));
  }

  @Test
  public void configurationFileChanged() throws IOException {
    File file = createTempFile("tracing", "test");
    Path testFile = Paths.get(file.getPath());
    Path originalConfigFile = Paths.get(getClass().getResource("/conf/test-original.conf").getPath());
    Path overwrittenConfigFile = Paths.get(getClass().getResource("/conf/test-overwritten.conf").getPath());
    copy(originalConfigFile, testFile, REPLACE_EXISTING);

    final TestFileSpanExporterConfiguration testFileSpanExporterConfiguration =
        new TestFileSpanExporterConfiguration(mock(MuleContext.class), "", file.getAbsolutePath());

    testFileSpanExporterConfiguration.doOnChange(() -> {
      testFileSpanExporterConfiguration.initialiseProperties();
      testFileSpanExporterConfiguration.changed = true;
    });
    assertThat(testFileSpanExporterConfiguration.getStringValue("key"), equalTo("value"));
    copy(overwrittenConfigFile, testFile, REPLACE_EXISTING);
    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> testFileSpanExporterConfiguration.changed));
    assertThat(testFileSpanExporterConfiguration.getStringValue("key"), equalTo("value-overwritten"));
  }

  /**
   * {@link FileSpanExporterConfiguration} used for testing properties file.
   */
  private static class TestFileSpanExporterConfiguration extends FileSpanExporterConfiguration {

    private final String confFolder;
    private final String confFileName;
    private boolean changed;

    public TestFileSpanExporterConfiguration(MuleContext muleContext, String confFolder, String confFileName) {
      super(muleContext);
      this.confFolder = confFolder;
      this.confFileName = confFileName;
    }

    @Override
    protected String getPropertiesFileName() {
      return confFileName;
    }

    @Override
    protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
      return Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected String getConfFolder() {
      return confFolder;
    }
  }
}
