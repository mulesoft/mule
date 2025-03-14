/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.config;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.PARENTBASED_TRACEIDRATIO_SAMPLER;

import static java.util.Collections.synchronizedList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.ast.api.exception.PropertyNotFoundException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.impl.FileSpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

public class OpenTelemetryAutoConfigurableSpanExporterConfiguration implements SpanExporterConfiguration, Disposable {

  private static final Logger LOGGER = getLogger(OpenTelemetryAutoConfigurableSpanExporterConfiguration.class);

  @Inject
  private MuleContext muleContext;

  private static final String DEFAULT_BACKOFF_MULTIPLIER = "1.5";
  private static final String DEFAULT_INITIAL_BACKOFF = "1";
  private static final String DEFAULT_MAXIMUM_BACKOFF = "5";
  private static final String DEFAULT_BACKOFF_MAX_ATTEMPTS = "5";
  private static final String GRPC_EXPORTER_TYPE = "GRPC";
  private static final String DEFAULT_EXPORTER_TYPE = GRPC_EXPORTER_TYPE;
  private static final String DEFAULT_GRPC_EXPORTER_ENDPOINT = "http://localhost:4317";
  private static final String DEFAULT_HTTP_EXPORTER_ENDPOINT = "http://localhost:4318/v1/traces";
  private static final String DEFAULT_EXPORTER_TIMEOUT = "10000";
  private static final String DEFAULT_STATISTICS_LOG_FREQUENCY = "30000";
  private static final String DEFAULT_BATCH_QUEUE_SIZE = "2048";
  private static final String DEFAULT_MAX_BATCH_SIZE = "512";
  private static final String DEFAULT_SCHEDULED_DELAY = "5000";

  private static final String DEFAULT_SAMPLER = PARENTBASED_TRACEIDRATIO_SAMPLER;
  private static final String DEFAULT_SAMPLER_ARG = "0.1";

  private SpanExporterConfiguration delegate;
  private final Map<String, String> defaultConfigurationValues = new HashMap<>();
  private final List<Runnable> runnablesOnChange = synchronizedList(new ArrayList<>());

  /**
   * This constructor is needed for injection in the registry.
   */
  public OpenTelemetryAutoConfigurableSpanExporterConfiguration() {}

  public OpenTelemetryAutoConfigurableSpanExporterConfiguration(SpanExporterConfiguration delegate) {
    this.delegate = delegate;
    initialiseDefaultConfigurationValues();
  }

  @Override
  public String getStringValue(String key) {
    try {
      if (delegate == null) {
        this.delegate = new CompositeSpanExporterConfiguration(Arrays.asList(new FileSpanExporterConfiguration(muleContext),
                                                                             new EnvironmentPropertiesConfiguration()));
        this.delegate.doOnConfigurationChanged(() -> runnablesOnChange.forEach(Runnable::run));
        initialiseDefaultConfigurationValues();
      }
      return delegate.getStringValue(key, defaultConfigurationValues.get(key));
    } catch (PropertyNotFoundException e) {
      return defaultConfigurationValues.get(key);
    }
  }

  private void initialiseDefaultConfigurationValues() {
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false");
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, DEFAULT_EXPORTER_TYPE);
    if (getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE).equals(GRPC_EXPORTER_TYPE)) {
      defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT, DEFAULT_GRPC_EXPORTER_ENDPOINT);
    } else {
      defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT, DEFAULT_HTTP_EXPORTER_ENDPOINT);
    }
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT, DEFAULT_EXPORTER_TIMEOUT);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER, DEFAULT_BACKOFF_MULTIPLIER);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF, DEFAULT_INITIAL_BACKOFF);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF, DEFAULT_MAXIMUM_BACKOFF);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS, DEFAULT_BACKOFF_MAX_ATTEMPTS);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY, DEFAULT_STATISTICS_LOG_FREQUENCY);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_SIZE);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE, DEFAULT_BATCH_QUEUE_SIZE);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY, DEFAULT_SCHEDULED_DELAY);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER, DEFAULT_SAMPLER);
    defaultConfigurationValues.put(MULE_OPEN_TELEMETRY_OTEL_TRACES_SAMPLER_ARG, DEFAULT_SAMPLER_ARG);
  }

  @Override
  public void doOnConfigurationChanged(Runnable doOnChange) {
    runnablesOnChange.add(doOnChange);
  }

  @Override
  public void dispose() {
    if (delegate != null) {
      disposeIfNeeded(delegate, LOGGER);
    }
  }
}
