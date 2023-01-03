/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.config;

import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;

import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import java.util.HashMap;
import java.util.Map;

public class OpenTelemetryAutoConfigurableSpanExporterConfiguration implements SpanExporterConfiguration {

  private static final String DEFAULT_BACKOFF_MULTIPLIER = "1.5";
  private static final String DEFAULT_INITIAL_BACKOFF = "1";
  private static final String DEFAULT_MAXIMUM_BACKOFF = "5";
  private static final String DEFAULT_BACKOFF_MAX_ATTEMPTS = "5";
  public static final String GRPC_EXPORTER_TYPE = "GRPC";
  public static final String DEFAULT_EXPORTER_TYPE = GRPC_EXPORTER_TYPE;
  public static final String DEFAULT_GRPC_EXPORTER_ENDPOINT = "http://localhost:4317";
  public static final String DEFAULT_HTTP_EXPORTER_ENDPOINT = "http://localhost:4318/v1/traces";
  public static final String DEFAULT_EXPORTER_TIMEOUT = "10";

  private final SpanExporterConfiguration delegate;
  private final Map<String, String> defaultConfigurationValues;

  public OpenTelemetryAutoConfigurableSpanExporterConfiguration(SpanExporterConfiguration delegate) {
    this.delegate = delegate;
    defaultConfigurationValues = getDefaultValues();
  }

  @Override
  public String getStringValue(String key) {
    return delegate.getStringValue(key, defaultConfigurationValues.getOrDefault(key, null));
  }

  private Map<String, String> getDefaultValues() {
    Map<String, String> defaultValues = new HashMap<>();
    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false");

    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, DEFAULT_EXPORTER_TYPE);
    if (getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE).equals(GRPC_EXPORTER_TYPE)) {
      defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT, DEFAULT_GRPC_EXPORTER_ENDPOINT);
    } else {
      defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT, DEFAULT_HTTP_EXPORTER_ENDPOINT);
    }
    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT, DEFAULT_EXPORTER_TIMEOUT);

    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER, DEFAULT_BACKOFF_MULTIPLIER);
    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF, DEFAULT_INITIAL_BACKOFF);
    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF, DEFAULT_MAXIMUM_BACKOFF);
    defaultValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS, DEFAULT_BACKOFF_MAX_ATTEMPTS);

    return defaultValues;
  }
}
