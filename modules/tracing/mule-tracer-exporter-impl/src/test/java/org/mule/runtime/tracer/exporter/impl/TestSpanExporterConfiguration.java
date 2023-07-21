/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import java.util.Map;

class TestSpanExporterConfiguration implements SpanExporterConfiguration {

  private final Map<String, String> properties;

  public TestSpanExporterConfiguration(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public String getStringValue(String key) {
    return properties.get(key);
  }
}
