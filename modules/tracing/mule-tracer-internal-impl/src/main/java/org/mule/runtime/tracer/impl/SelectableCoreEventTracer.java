/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.NoopCoreEventTracer.getNoopCoreEventTracer;

import static java.lang.Boolean.parseBoolean;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.ast.api.exception.PropertyNotFoundException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;

/**
 * An {@link SelectableCoreEventTracer<CoreEvent>} that switches among a noop and the implementation of the {@link EventTracer}
 * according to the configuration.
 *
 * @since 4.5.0
 */
public class SelectableCoreEventTracer implements EventTracer<CoreEvent>, Initialisable {

  private SpanExporterConfiguration spanExporterConfiguration;
  private CoreEventTracer coreEventTracer;
  private final static EventTracer<CoreEvent> NOOP_CORE_EVENT_TRACER = getNoopCoreEventTracer();
  private EventTracer<CoreEvent> selectedCoreEventTracer = NOOP_CORE_EVENT_TRACER;
  private FeatureFlaggingService featureFlaggingService;
  private EventSpanFactory eventSpanFactory;

  @Override
  public void initialise() throws InitialisationException {
    coreEventTracer = new CoreEventTracer(featureFlaggingService, eventSpanFactory);
    coreEventTracer.initialise();
    updateSelectedCoreEventTracer();
    spanExporterConfiguration
        .doOnConfigurationChanged(this::updateSelectedCoreEventTracer);
  }

  private synchronized void updateSelectedCoreEventTracer() {
    if (isTracingExportEnabled()) {
      this.selectedCoreEventTracer = coreEventTracer;
    } else {
      this.selectedCoreEventTracer = NOOP_CORE_EVENT_TRACER;
    }
  }

  private boolean isTracingExportEnabled() {
    try {
      return parseBoolean(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false"));
    } catch (PropertyNotFoundException e) {
      return false;
    }
  }

  @Override
  public Optional<Span> startSpan(CoreEvent event, InitialSpanInfo spanInfo) {
    return selectedCoreEventTracer.startSpan(event, spanInfo);
  }

  @Override
  public Optional<Span> startSpan(CoreEvent event, InitialSpanInfo spanInfo, Assertion assertion) {
    return selectedCoreEventTracer.startSpan(event, spanInfo, assertion);
  }

  @Override
  public void endCurrentSpan(CoreEvent event) {
    selectedCoreEventTracer.endCurrentSpan(event);
  }

  @Override
  public void endCurrentSpan(CoreEvent event, Assertion condition) {
    selectedCoreEventTracer.endCurrentSpan(event, condition);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    selectedCoreEventTracer.injectDistributedTraceContext(eventContext, distributedTraceContextGetter);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    selectedCoreEventTracer.recordErrorAtCurrentSpan(event, errorSupplier, isErrorEscapingCurrentSpan);
  }

  @Override
  public void setCurrentSpanName(CoreEvent event, String name) {
    selectedCoreEventTracer.setCurrentSpanName(event, name);
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent event, String key, String value) {
    selectedCoreEventTracer.addCurrentSpanAttribute(event, key, value);
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent event, Map<String, String> attributes) {
    selectedCoreEventTracer.addCurrentSpanAttributes(event, attributes);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, boolean isErrorEscapingCurrentSpan) {
    selectedCoreEventTracer.recordErrorAtCurrentSpan(event, isErrorEscapingCurrentSpan);
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return selectedCoreEventTracer.getDistributedTraceContextMap(event);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return selectedCoreEventTracer.getSpanSnifferManager();
  }

  @Inject
  public void setSpanExporterConfiguration(SpanExporterConfiguration spanExporterConfiguration) {
    this.spanExporterConfiguration = spanExporterConfiguration;
  }

  @Inject
  public void setEventSpanFactory(EventSpanFactory eventSpanFactory) {
    this.eventSpanFactory = eventSpanFactory;
  }

  @Inject
  public void setFeatureFlaggingService(FeatureFlaggingService featureFlaggingService) {
    this.featureFlaggingService = featureFlaggingService;
  }

  public EventTracer<CoreEvent> getSelectedCoreEventTracer() {
    return selectedCoreEventTracer;
  }
}
