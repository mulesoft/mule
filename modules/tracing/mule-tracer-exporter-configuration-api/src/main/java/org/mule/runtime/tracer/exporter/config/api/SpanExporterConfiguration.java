/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.config.api;

/**
 * A configuration for the export of spans.
 *
 * @since 4.5.0
 */
public interface SpanExporterConfiguration {

  /**
   * Returns the value of a configuration parameter.
   * 
   * @param key the key of a configuration parameter.
   * @return the value associated to the {@param key} or null if not found.
   */
  String getStringValue(String key);

  default String getStringValue(String key, String defaultValue) {
    String value = getStringValue(key);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * @param doOnConfigurationChanged to execute when a change in the configuration is detected.
   */
  default void doOnConfigurationChanged(Runnable doOnConfigurationChanged) {}
}
