/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.api.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.getPropagator;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.getTracer;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.config.SystemPropertiesSpanExporterConfiguration;

import javax.annotation.Nullable;
import java.util.Map;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * A Provider for {@link MuleOpenTelemetrySpan}.
 *
 * @since 4.5.0
 */
public class MuleOpenTelemetrySpanProvider {

  private static final SpanExporterConfiguration CONFIGURATION = new SystemPropertiesSpanExporterConfiguration();

  private static final TextMapGetter<Map<String, String>> OPEN_TELEMETRY_SPAN_GETTER = new MuleOpenTelemetryRemoteContextGetter();

  private MuleOpenTelemetrySpanProvider() {}

  public static MuleOpenTelemetrySpan getNewOpenTelemetrySpan(InternalSpan internalSpan,
                                                              InitialSpanInfo initialSpanInfo,
                                                              String serviceNAme) {

    InitialExportInfo initialExportInfo = initialSpanInfo.getInitialExportInfo();

    if (!initialExportInfo.isExportable()) {
      return getNonExportableSpan(internalSpan);
    }

    return getExportableSpan(internalSpan, initialExportInfo, serviceNAme, initialSpanInfo.isPolicySpan(),
                             initialSpanInfo.isRootSpan());
  }

  private static MuleOpenTelemetrySpan getExportableSpan(InternalSpan internalSpan,
                                                         InitialExportInfo initialExportInfo,
                                                         String serviceName,
                                                         boolean isPolicy,
                                                         boolean isRoot) {
    SpanBuilder spanBuilder = getTracer(CONFIGURATION, serviceName).spanBuilder(internalSpan.getName())
        .setStartTimestamp(internalSpan.getDuration().getStart(), NANOSECONDS);

    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    // Configure the parent span.
    if (parentSpan != null) {
      SpanExporter spanExporter = parentSpan.getSpanExporter();

      if (spanExporter instanceof OpenTelemetrySpanExporter) {
        spanBuilder.setParent(Context.current().with(((OpenTelemetrySpanExporter) spanExporter).getOpenTelemetrySpan()));
      } else {
        spanBuilder.setParent(getPropagator().getTextMapPropagator().extract(Context.current(), parentSpan.serializeAsMap(),
                                                                             OPEN_TELEMETRY_SPAN_GETTER));
      }
    }

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan(spanBuilder.startSpan());
    decoratedMuleOpenTelemetrySpan.setNoExportUntil(initialExportInfo.noExportUntil());
    decoratedMuleOpenTelemetrySpan
        .setRoot(isRoot);
    decoratedMuleOpenTelemetrySpan.setPolicy(isPolicy);
    return decoratedMuleOpenTelemetrySpan;
  }

  private static MuleOpenTelemetrySpan getNonExportableSpan(InternalSpan internalSpan) {
    InternalSpan parentSpan = getAsInternalSpan(internalSpan.getParent());

    // Configure the parent span.
    if (parentSpan != null) {
      SpanExporter parentSpanSpanExporter = parentSpan.getSpanExporter();

      if (parentSpanSpanExporter instanceof OpenTelemetrySpanExporter) {
        return new NoopMuleOpenTelemetrySpan(((OpenTelemetrySpanExporter) parentSpanSpanExporter).getOpenTelemetrySpan());
      }
    }

    return new NoopMuleOpenTelemetrySpan();
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

}
