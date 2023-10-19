/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.config;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.opentelemetry.sdk.common.export.RetryPolicy;

public class SpanExporterConfigurationUtils {

  private SpanExporterConfigurationUtils() {}

  /**
   * <p>
   * Enables the experimental backoff strategy for the Open Telemetry exporters.
   * </p>
   * <p>
   * A default parametrization will be used if no configuration is provided:
   * </p>
   * <lu>
   * <li>Initial backoff: 1 Second</li>
   * <li>Maximum backoff: 5 Seconds</li>
   * <li>Maximum retry attempts: 5</li>
   * <li>Backoff multiplier: 1.5</li> </lu>
   *
   * @param exporterBuilder           The exporter builder.
   * @param spanExporterConfiguration The span exporter configuration that will be used to override the default backoff
   *                                  parameters.
   */
  public static void enableBackoffStrategy(Object exporterBuilder, SpanExporterConfiguration spanExporterConfiguration) {
    try {
      // Since it's an experimental feature, we must enable it using reflection.
      // See https://github.com/open-telemetry/opentelemetry-java/pull/3791
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

  /**
   * Builder that encapsulates the configuration of the Open Telemetry {@link RetryPolicy}.
   */
  private static class RetryPolicyBuilder {

    private final SpanExporterConfiguration spanExporterConfiguration;

    public RetryPolicyBuilder(SpanExporterConfiguration spanExporterConfiguration) {
      this.spanExporterConfiguration = spanExporterConfiguration;
    }

    public RetryPolicy build() {
      return RetryPolicy.builder()
          .setBackoffMultiplier(parseDouble(spanExporterConfiguration
              .getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER)))
          .setInitialBackoff(ofSeconds(parseLong(spanExporterConfiguration
              .getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF))))
          .setMaxBackoff(ofSeconds(parseLong(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF))))
          .setMaxAttempts(Integer
              .parseInt(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS)))
          .build();
    }
  }
}
