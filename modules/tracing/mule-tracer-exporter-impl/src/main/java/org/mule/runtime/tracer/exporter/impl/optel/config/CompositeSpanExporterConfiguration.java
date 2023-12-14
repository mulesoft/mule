/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.config;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import java.util.List;

/**
 * A composite {@link SpanExporterConfiguration}.
 */
public class CompositeSpanExporterConfiguration implements SpanExporterConfiguration {

  private final List<SpanExporterConfiguration> spanExporterConfigurations;

  public CompositeSpanExporterConfiguration(List<SpanExporterConfiguration> spanExporterConfigurations) {
    this.spanExporterConfigurations = spanExporterConfigurations;
  }

  @Override
  public String getStringValue(String key) {
    return spanExporterConfigurations.stream()
        .filter(configuration -> configuration.getStringValue(key) != null)
        .findFirst()
        .map(configuration -> configuration.getStringValue(key))
        .orElse(null);
  }

  @Override
  public String getStringValue(String key, String defaultValue) {
    return spanExporterConfigurations.stream()
        .filter(configuration -> configuration.getStringValue(key) != null)
        .findFirst()
        .map(configuration -> configuration.getStringValue(key))
        .orElse(defaultValue);
  }

  @Override
  public void doOnConfigurationChanged(Runnable doOnConfigurationChanged) {
    spanExporterConfigurations.forEach(configuration -> configuration.doOnConfigurationChanged(doOnConfigurationChanged));
  }
}
