/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.configuration;

import org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import java.util.HashMap;
import java.util.Map;

public class AutoConfigurableSpanExporterConfiguration implements SpanExporterConfiguration {

  private final SpanExporterConfiguration delegate;
  private static final Map<String, String> defaultValues = getDefaultValues();

  public AutoConfigurableSpanExporterConfiguration(SpanExporterConfiguration delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getStringValue(String key) {
    return delegate.getStringValue(key, defaultValues.getOrDefault(key, null));
  }

  private static Map<String, String> getDefaultValues() {
    Map<String, String> defaultValues = new HashMap<>();
    defaultValues.put(OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false");
    defaultValues.put(OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE, "GRPC");
    return defaultValues;
  }
}
