/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_DEFAULT_TRACING_LEVEL;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.valueOf;

import static java.lang.System.getProperty;
import static java.util.Collections.synchronizedList;

import org.mule.runtime.ast.api.exception.PropertyNotFoundException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

public class OpenTelemetryAutoConfigurableTracingLevelConfiguration implements TracingLevelConfiguration {

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
  public OpenTelemetryAutoConfigurableTracingLevelConfiguration() {}

  public OpenTelemetryAutoConfigurableTracingLevelConfiguration(TracingLevelConfiguration delegate) {
    this.delegate = delegate;
  }

  @Inject
  public void setSpanExporterConfiguration(SpanExporterConfiguration spanExporterConfiguration) {
    this.spanExporterConfiguration = spanExporterConfiguration;
  }

  @Override
  public TracingLevel getTracingLevel() {
    try {
      if (delegate == null) {
        this.delegate = new FileTracingLevelConfiguration(muleContext);
        ((FileTracingLevelConfiguration) delegate).setSpanExporterConfiguration(spanExporterConfiguration);
        consumersOnChange.forEach(consumer -> this.delegate.onConfigurationChange(consumer));
      }
      return delegate.getTracingLevel(defaultLevel);
    } catch (PropertyNotFoundException e) {
      return defaultLevel;
    }
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    try {
      if (delegate == null) {
        this.delegate = new FileTracingLevelConfiguration(muleContext);
        consumersOnChange.forEach(consumer -> this.delegate.onConfigurationChange(consumer));
      }
      return delegate.getTracingLevelOverride(location, defaultLevel);
    } catch (PropertyNotFoundException e) {
      return defaultLevel;
    }
  }

  @Override
  public void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer) {
    consumersOnChange.add(onConfigurationChangeConsumer);
  }
}
