/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.config.api;

/**
 * Configuration to export metrics.
 * 
 * @since 4.5.0
 */
public interface MeterExporterConfiguration {

  /**
   * @param key the key of a configuration parameter
   *
   * @return the value associated to the {@param key}
   */
  String getStringValue(String key);

  default String getStringValue(String key, String defaultValue) {
    String value = getStringValue(key);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }
}
