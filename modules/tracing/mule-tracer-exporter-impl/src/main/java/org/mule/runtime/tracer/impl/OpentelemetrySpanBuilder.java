/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.config.SystemPropertiesSpanExporterConfiguration;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.impl.OpenTelemetryResources.getPropagator;
import static org.mule.runtime.tracer.impl.OpenTelemetryResources.getTracer;

public class OpentelemetrySpanBuilder {

  private static final SpanExporterConfiguration CONFIGURATION = new SystemPropertiesSpanExporterConfiguration();

  private static final TextMapGetter<Map<String, String>> OPEN_TELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  public static MuleOpenTelemetrySpan getNewOpenTelemetrySpan(InternalSpan internalSpan,
                                                              StartExportInfo exportSpanCustomizationInfo,
                                                              String serviceName,
                                                              boolean isPolicy,
                                                              boolean isRoot) {
    if (!exportSpanCustomizationInfo.isExportable()) {
      return getNonExportableSpan(internalSpan, exportSpanCustomizationInfo, isPolicy, isRoot);
    }

    return getExportableSpan(internalSpan, exportSpanCustomizationInfo, serviceName, isPolicy, isRoot);
  }

  private static MuleOpenTelemetrySpan getExportableSpan(InternalSpan internalSpan,
                                                         StartExportInfo exportSpanCustomizationInfo,
                                                         String serviceName,
                                                         boolean isPolicy,
                                                         boolean isRoot) {
    SpanBuilder spanBuilder = getTracer(CONFIGURATION, serviceName).spanBuilder(internalSpan.getName())
        .setStartTimestamp(internalSpan.getDuration().getStart(), NANOSECONDS);

    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    if (parentSpan != null) {
      SpanExporter spanExporter = parentSpan.getSpanExporter();

      if (spanExporter instanceof OpenTelemetrySpanExporter) {
        spanBuilder.setParent(Context.current().with(((OpenTelemetrySpanExporter) spanExporter).getOpenTelemetrySpan()));
      } else {
        spanBuilder.setParent(getPropagator().getTextMapPropagator().extract(Context.current(), parentSpan.serializeAsMap(),
                                                                             OPEN_TELEMETRY_SPAN_GETTER));
      }
    }

    DelegateMuleOpenTelemetrySpan delegateMuleOpenTelemetrySpan = new DelegateMuleOpenTelemetrySpan(spanBuilder.startSpan());
    delegateMuleOpenTelemetrySpan.setNoExportUntil(exportSpanCustomizationInfo.noExportUntil());
    delegateMuleOpenTelemetrySpan
        .setCustomizableInformationCarrier(isRoot);
    delegateMuleOpenTelemetrySpan.setNotIntercepting(isPolicy);
    return delegateMuleOpenTelemetrySpan;
  }

  private static MuleOpenTelemetrySpan getNonExportableSpan(InternalSpan internalSpan,
                                                            StartExportInfo exportSpanCustomizationInfo,
                                                            boolean isPolicy,
                                                            boolean isRoot) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());
    NonExportableSpan nonExportableSpan = null;
    if (parentSpan != null) {
      SpanExporter parentSpanSpanExporter = parentSpan.getSpanExporter();

      if (parentSpanSpanExporter instanceof OpenTelemetrySpanExporter) {
        nonExportableSpan = new NonExportableSpan(((OpenTelemetrySpanExporter) parentSpanSpanExporter).getOpenTelemetrySpan());
      }
    } else {
      nonExportableSpan = new NonExportableSpan();
    }

    nonExportableSpan.setNoExportUntil(exportSpanCustomizationInfo.noExportUntil());
    return nonExportableSpan;
  }

  /**
   * An Internal {@link TextMapGetter} to retrieve the remote span context.
   *
   * This is used to resolve a remote OpTel Span propagated through W3C Trace Context.
   */
  private static class MuleOpenTelemetryRemoteContextGetter implements TextMapGetter<Map<String, String>> {

    @Override
    public Iterable<String> keys(Map<String, String> stringStringMap) {
      return stringStringMap.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, String> stringStringMap, @Nullable String key) {
      if (stringStringMap == null) {
        return null;
      }

      return stringStringMap.get(key);
    }
  }


  private static class NonExportableSpan implements MuleOpenTelemetrySpan {

    private MuleOpenTelemetrySpan rootSpan;
    private Set<String> noExportableUntil = Collections.emptySet();

    public NonExportableSpan(
                             MuleOpenTelemetrySpan rootSpan) {
      this.rootSpan = rootSpan;
    }

    public NonExportableSpan() {}

    @Override
    public Context getSpanOpenTelemetryContext() {
      if (rootSpan == null) {
        return Context.current();
      }
      return Context.current().with(rootSpan);
    }

    @Override
    public void end(InternalSpan internalSpan) {

    }

    @Override
    public Set<String> getNoExportUntil() {
      return noExportableUntil;
    }

    @Override
    public Map<String, String> getDistributedTraceContextMap() {
      return rootSpan.getDistributedTraceContextMap();
    }

    @Override
    public void setNoExportUntil(Set<String> noExportableUntil) {
      this.noExportableUntil = noExportableUntil;
    }

    @Override
    public void setNotIntercepting(boolean notIntercepting) {
      // Nothing to do
    }

    @Override
    public void setCustomizableInformationCarrier(boolean propagateSpanFromParent) {
      // Nothing to do.
    }

    @Override
    public boolean isNotIntercepting() {
      return true;
    }

    @Override
    public boolean isSetCustomizableInformationCarrier() {
      return false;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> attributeKey, T t) {
      return this;
    }

    @Override
    public Span addEvent(String s, Attributes attributes) {
      return this;
    }

    @Override
    public Span addEvent(String s, Attributes attributes, long l, TimeUnit timeUnit) {
      return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String s) {
      return this;
    }

    @Override
    public Span recordException(Throwable throwable, Attributes attributes) {
      return this;
    }

    @Override
    public Span updateName(String s) {
      return this;
    }

    @Override
    public void end() {

    }

    @Override
    public void end(long l, TimeUnit timeUnit) {

    }

    @Override
    public SpanContext getSpanContext() {
      if (rootSpan != null) {
        return rootSpan.getSpanContext();
      }
      return SpanContext.getInvalid();
    }

    @Override
    public boolean isRecording() {
      return false;
    }
  }
}
