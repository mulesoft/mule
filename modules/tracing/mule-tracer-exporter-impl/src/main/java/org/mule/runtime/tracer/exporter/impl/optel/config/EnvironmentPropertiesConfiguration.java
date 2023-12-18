/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.config;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterEnvProperties.OTEL_TRACES_SAMPLER_ARG_ENV;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterEnvProperties.OTEL_TRACES_SAMPLER_ENV;

import static java.lang.System.getenv;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SpanExporterConfiguration} based on env properties.
 */
public class EnvironmentPropertiesConfiguration implements SpanExporterConfiguration {

  Map<String, String> envPropertiesMatching = new HashMap<String, String>() {

    {
      put(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER, OTEL_TRACES_SAMPLER_ENV);
      put(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG, OTEL_TRACES_SAMPLER_ARG_ENV);
    }
  };

  @Override
  public String getStringValue(String key) {
    return getenv(propertyToEnvKey(key));
  }

  @Override
  public String getStringValue(String key, String defaultValue) {
    String envValue = getenv(propertyToEnvKey(key));
    if (envValue == null) {
      envValue = defaultValue;
    }
    return envValue;
  }

  @Override
  public void doOnConfigurationChanged(Runnable doOnConfigurationChanged) {
    // Nothing to do.
  }

  private String propertyToEnvKey(String key) {
    return envPropertiesMatching.getOrDefault(key, key);
  }
}
