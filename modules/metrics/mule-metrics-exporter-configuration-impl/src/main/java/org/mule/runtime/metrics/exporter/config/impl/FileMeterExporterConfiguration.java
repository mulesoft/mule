/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.config.impl;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION;

import static java.lang.System.getProperty;
import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.observability.FileConfiguration;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

/**
 * A {@link MeterExporterConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileMeterExporterConfiguration extends FileConfiguration implements MeterExporterConfiguration {

  private static final Logger LOGGER = getLogger(FileMeterExporterConfiguration.class);
  private static final String CONFIGURATION_FILE_NAME = "meter-exporter.conf";

  private final MuleContext muleContext;
  private JsonNode configuration;
  private final ConfigurationPropertiesResolver propertyResolver =
      new DefaultConfigurationPropertiesResolver(empty(), new SystemPropertiesConfigurationProvider());

  public FileMeterExporterConfiguration(MuleContext muleContext) {
    super(muleContext);
    this.muleContext = muleContext;
    getMeterExporterProperties();
  }

  protected boolean isAValueCorrespondingToAPath(String key) {
    return key.equals(MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION) ||
        key.equals(MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION);
  }

  @Override
  protected JsonNode getConfiguration() {
    return configuration;
  }

  @Override
  protected ConfigurationPropertiesResolver getPropertyResolver() {
    return propertyResolver;
  }

  private void getMeterExporterProperties() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider.getResourceAsStream(resolveConfigurationFilePath());
      configuration = loadConfiguration(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER.info("No meter exporter configuration found.");
    }
  }

  private String resolveConfigurationFilePath() {
    String defaultConfigurationFilePath = getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName();
    return getProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH, defaultConfigurationFilePath);
  }

  protected String getPropertiesFileName() {
    return CONFIGURATION_FILE_NAME;
  }
}
