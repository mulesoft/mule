/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

import java.util.Map;

/**
 * A {@link MeterExporterConfiguration} for testing.
 */
public class TestMeterExporterConfiguration implements MeterExporterConfiguration {

  private final Map<String, String> properties;

  public TestMeterExporterConfiguration(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public String getStringValue(String key) {
    return properties.get(key);
  }
}
