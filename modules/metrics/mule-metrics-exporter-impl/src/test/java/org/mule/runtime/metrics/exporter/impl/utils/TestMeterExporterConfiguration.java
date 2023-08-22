/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
