/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import static org.mule.runtime.core.internal.profiling.NoopCoreEventTracer.getNoopCoreEventTracer;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;

import static java.lang.Boolean.parseBoolean;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.TogglableEventTracer;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.inject.Inject;

/**
 * An {@link EventTracer} that can be enabled/disabled
 */
public class ToggleableCoreEventTracer implements TogglableEventTracer<CoreEvent>, Initialisable {

  @Inject
  private EventSpanFactory eventSpanFactory;

  @Inject
  FeatureFlaggingService featureFlaggingService;
  private EventTracer<CoreEvent> coreEventTracer;

  private EventTracer<CoreEvent> noopCoreEventTracer;

  private EventTracer<CoreEvent> selectedEventTracer;

  @Inject
  private SpanExporterConfiguration configuration;

  @Override
  public void initialise() throws InitialisationException {
    coreEventTracer = new CoreEventTracer(eventSpanFactory, featureFlaggingService);
    noopCoreEventTracer = getNoopCoreEventTracer();
    if (parseBoolean(configuration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED))) {
      selectedEventTracer = coreEventTracer;
    } else {
      selectedEventTracer = noopCoreEventTracer;
    }
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo spanInfo) {
    return selectedEventTracer.startComponentSpan(event, spanInfo);
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent event, InitialSpanInfo spanInfo, Assertion assertion) {
    return selectedEventTracer.startComponentSpan(event, spanInfo, assertion);
  }

  @Override
  public void endCurrentSpan(CoreEvent event) {
    selectedEventTracer.endCurrentSpan(event);
  }

  @Override
  public void endCurrentSpan(CoreEvent event, Assertion condition) {
    selectedEventTracer.endCurrentSpan(event, condition);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    selectedEventTracer.injectDistributedTraceContext(eventContext, distributedTraceContextGetter);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent event, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    selectedEventTracer.recordErrorAtCurrentSpan(event, errorSupplier, isErrorEscapingCurrentSpan);
  }

  @Override
  public void setCurrentSpanName(CoreEvent event, String name) {
    selectedEventTracer.setCurrentSpanName(event, name);
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent event, String key, String value) {
    selectedEventTracer.addCurrentSpanAttribute(event, key, value);
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent event, Map<String, String> attributes) {
    selectedEventTracer.addCurrentSpanAttributes(event, attributes);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return selectedEventTracer.getSpanSnifferManager();
  }

  @Override
  public synchronized void toggle(boolean enableTracing) {
    if (enableTracing) {
      selectedEventTracer = coreEventTracer;
    } else {
      selectedEventTracer = noopCoreEventTracer;
    }
  }
}
