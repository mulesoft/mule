/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
