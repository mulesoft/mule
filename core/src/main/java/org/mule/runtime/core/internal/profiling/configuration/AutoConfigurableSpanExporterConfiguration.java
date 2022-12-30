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
