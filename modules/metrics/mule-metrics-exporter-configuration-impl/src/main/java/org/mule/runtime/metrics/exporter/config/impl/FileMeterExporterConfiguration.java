/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.config.impl;

import static java.util.Optional.empty;
import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION;

import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * A {@link MeterExporterConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileMeterExporterConfiguration implements MeterExporterConfiguration {

  private static final Logger LOGGER = getLogger(FileMeterExporterConfiguration.class);
  private static final String CONFIGURATION_FILE_NAME = "meter-exporter.conf";

  private final MuleContext muleContext;
  private final Properties properties;

  private final ConfigurationPropertiesResolver propertyResolver =
      new DefaultConfigurationPropertiesResolver(empty(), new SystemPropertiesConfigurationProvider());

  public FileMeterExporterConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.properties = getMeterExporterProperties();
  }

  @Override
  public String getStringValue(String key) {
    String value = properties.getProperty(key);

    if (value != null) {
      // Resolves the actual value when the current one is a system property reference.
      value = propertyResolver.apply(value);

      if (isAValueCorrespondingToAPath(key)) {
        // Obtain absolute path and return it if possible.
        String absolutePath = getAbsolutePath(value);

        if (absolutePath != null) {
          return absolutePath;
        }
        // Otherwise, return the non-resolved value so that the error message makes sense.
      }
    }
    return value;
  }

  private boolean isAValueCorrespondingToAPath(String key) {
    return key.equals(MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION) ||
        key.equals(MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION);
  }

  private String getAbsolutePath(String value) {
    Path path = Paths.get(value);

    try {
      if (!path.isAbsolute()) {
        URL url = getExecutionClassLoader(muleContext).getResource(value);

        if (url != null) {
          return new File(url.toURI()).getAbsolutePath();
        }
      }
    } catch (URISyntaxException e) {
      return value;
    }

    return null;
  }

  private Properties getMeterExporterProperties() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider.getResourceAsStream(resolveConfigurationFilePath());
      return loadProperties(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER.info("No meter exporter configuration found in the conf directory.");
    }
    return new Properties();
  }

  private String resolveConfigurationFilePath() {
    String defaultConfigurationFilePath = getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName();
    return getProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH, defaultConfigurationFilePath);
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

  protected String getPropertiesFileName() {
    return CONFIGURATION_FILE_NAME;
  }
}
