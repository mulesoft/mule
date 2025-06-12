/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.valueOf;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.Objects.requireNonNull;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * Autoconfiguration of tracing level. Will use the default level.
 *
 * @see OpenTelemetrySpanExporterConfigurationProperties#MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL
 */
public class AutoConfigurableTracingLevelConfiguration implements TracingLevelConfiguration, Disposable {

  private static final Logger LOGGER = getLogger(AutoConfigurableTracingLevelConfiguration.class);

  private MuleContext muleContext;
  private SpanExporterConfiguration spanExporterConfiguration;
  private TracingLevelConfiguration delegate;
  private TracingLevel defaultLevel =
      valueOf(getProperty(MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL, MONITORING.toString()).toUpperCase());

  @Inject
  public AutoConfigurableTracingLevelConfiguration(MuleContext muleContext) {
    this(muleContext, new FileTracingLevelConfiguration(muleContext));
  }

  protected AutoConfigurableTracingLevelConfiguration(MuleContext muleContext, TracingLevelConfiguration delegate) {
    requireNonNull(delegate, "delegate cannot be null");

    this.muleContext = muleContext;
    this.delegate = delegate;
  }

  @Inject
  public void setSpanExporterConfiguration(SpanExporterConfiguration spanExporterConfiguration) {
    this.spanExporterConfiguration = spanExporterConfiguration;
  }

  @Override
  public TracingLevel getTracingLevel() {
    return getTracingLevelFromDelegate(() -> delegate.getTracingLevel());
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    return getTracingLevelFromDelegate(() -> delegate.getTracingLevelOverride(location));
  }

  private TracingLevel getTracingLevelFromDelegate(Supplier<TracingLevel> tracingLevelSupplier) {
    try {
      TracingLevel level = tracingLevelSupplier.get();
      return level != null ? level : defaultLevel;
    } catch (MuleRuntimeException e) {
      if (parseBoolean(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false"))) {
        LOGGER.info("Non existent or non parseable tracing level config file found. "
            + "The tracing level will be set to the default: {}. Enable DEBUG log level to see the exception",
                    defaultLevel);
        LOGGER.debug("Exception:", e);
      }
    }
    return defaultLevel;
  }

  @Override
  public void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer) {
    delegate.onConfigurationChange(onConfigurationChangeConsumer);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }
}
