package org.mule.runtime.tracer.impl.exporter.config;

import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.opentelemetry.exporter.internal.retry.RetryPolicy;

public class SpanExporterConfigUtils {

  private SpanExporterConfigUtils() {}

  public static void enableBackoffStrategy(Object exporterBuilder, SpanExporterConfiguration spanExporterConfiguration) {
    try {
      Field delegateField = exporterBuilder.getClass().getDeclaredField("delegate");
      delegateField.setAccessible(true);
      Method setRetryPolicyMethod =
          delegateField.get(exporterBuilder).getClass().getDeclaredMethod("setRetryPolicy", RetryPolicy.class);
      setRetryPolicyMethod.setAccessible(true);
      setRetryPolicyMethod.invoke(delegateField.get(exporterBuilder), new RetryPolicyBuilder(spanExporterConfiguration).build());
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new MuleRuntimeException(new IllegalArgumentException("Unable to set the RetryPolicy reflectively.", e));
    }
  }

  private static class RetryPolicyBuilder {

    private static final String DEFAULT_BACKOFF_MULTIPLIER = "1.5";
    private static final String DEFAULT_INITIAL_BACKOFF = "1";
    private static final String DEFAULT_MAXIMUM_BACKOFF = "5";
    private static final String DEFAULT_MAX_ATTEMPTS = "5";

    private final SpanExporterConfiguration spanExporterConfiguration;

    public RetryPolicyBuilder(SpanExporterConfiguration spanExporterConfiguration) {
      this.spanExporterConfiguration = spanExporterConfiguration;
    }

    public RetryPolicy build() {
      return RetryPolicy.builder()
          .setBackoffMultiplier(parseDouble(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER,
                  DEFAULT_BACKOFF_MULTIPLIER)))
          .setInitialBackoff(ofSeconds(parseLong(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF,
                  DEFAULT_INITIAL_BACKOFF))))
          .setMaxBackoff(ofSeconds(parseLong(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF, DEFAULT_MAXIMUM_BACKOFF))))
          .setMaxAttempts(Integer
              .parseInt(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS)))
          .build();
    }
  }
}
