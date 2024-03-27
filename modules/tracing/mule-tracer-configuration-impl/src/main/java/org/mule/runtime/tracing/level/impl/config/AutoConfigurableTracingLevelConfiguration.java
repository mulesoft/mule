/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.valueOf;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.synchronizedList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Autoconfiguration of tracing level. Will use MONITORING as the default level if there is not one configured in a config file.
 */
public class AutoConfigurableTracingLevelConfiguration implements TracingLevelConfiguration {

  private static final Logger LOGGER = getLogger(AutoConfigurableTracingLevelConfiguration.class);

  @Inject
  private MuleContext muleContext;
  private SpanExporterConfiguration spanExporterConfiguration;
  private TracingLevelConfiguration delegate;
  private TracingLevel defaultLevel =
      valueOf(getProperty(MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL, MONITORING.toString()).toUpperCase());

  private final List<Consumer<TracingLevelConfiguration>> consumersOnChange = synchronizedList(new ArrayList<>());

  /**
   * This constructor is needed for injection in the registry.
   */
  public AutoConfigurableTracingLevelConfiguration() {}

  public AutoConfigurableTracingLevelConfiguration(TracingLevelConfiguration delegate) {
    this.delegate = delegate;
  }

  @Inject
  public void setSpanExporterConfiguration(SpanExporterConfiguration spanExporterConfiguration) {
    this.spanExporterConfiguration = spanExporterConfiguration;
  }

  @Override
  public TracingLevel getTracingLevel() {
    return getTracingLevel(false, null);
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    return getTracingLevel(true, location);
  }

  public TracingLevel getTracingLevel(boolean isOverride, String location) {
    try {
      if (delegate == null) {
        this.delegate = new FileTracingLevelConfiguration(muleContext);
        consumersOnChange.forEach(consumer -> this.delegate.onConfigurationChange(consumer));
      }
      TracingLevel level = isOverride ? delegate.getTracingLevelOverride(location) : delegate.getTracingLevel();
      return level != null ? level : defaultLevel;
    } catch (IllegalArgumentException e) {
      LOGGER.error("The tracing level will be set to the default level.");
    } catch (MuleRuntimeException e) {
      if (parseBoolean(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false"))) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER
              .debug(format("Non existent or non parseable tracing level config file found. The tracing level will be set to the default: %s",
                            defaultLevel),
                     e);
        } else {
          LOGGER
              .info(format("Non existent or non parseable tracing level config file found. The tracing level will be set to the default: %s. Enable DEBUG log level to see the exception",
                           defaultLevel));
        }
      }
    }
    return defaultLevel;
  }

  @Override
  public void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer) {
    consumersOnChange.add(onConfigurationChangeConsumer);
  }
}
