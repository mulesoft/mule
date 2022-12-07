/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.capturer;


import static java.lang.String.valueOf;

import static io.opentelemetry.api.trace.StatusCode.ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

/**
 * A {@link SpanExporter} that captures Open Telemetry exported spans.
 *
 * @since 4.5.0
 */
public class CapturingSpanExporterWrapper implements SpanExporter {

  private final SpanExporter delegate;
  private Set<MuleSpanSniffer> spanCapturers = ConcurrentHashMap.newKeySet();

  public CapturingSpanExporterWrapper(SpanExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> collection) {
    if (!spanCapturers.isEmpty()) {
      spanCapturers.forEach(capturer -> capturer.addSpans(collection));
    }
    return delegate.export(collection);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  public ExportedSpanSniffer getSpanCapturer() {
    MuleSpanSniffer spanCapturer = new MuleSpanSniffer(this);
    spanCapturers.add(spanCapturer);
    return spanCapturer;
  }

  private void dispose(ExportedSpanSniffer muleSpanCapturer) {
    spanCapturers.remove(muleSpanCapturer);
  }

  private static final class MuleSpanSniffer implements ExportedSpanSniffer {

    private final CapturingSpanExporterWrapper muleOtlpGrpcSpanExporter;
    private Set<SpanData> spanData = ConcurrentHashMap.newKeySet();

    public MuleSpanSniffer(CapturingSpanExporterWrapper muleOtlpGrpcSpanExporter) {
      this.muleOtlpGrpcSpanExporter = muleOtlpGrpcSpanExporter;
    }

    public void addSpans(Collection<SpanData> spanItems) {
      spanData.addAll(spanItems);
    }

    @Override
    public Collection<CapturedExportedSpan> getExportedSpans() {
      List<CapturedExportedSpan> exportedSpans = new ArrayList<>();
      for (SpanData spanData : spanData) {
        exportedSpans.add(new SpanDataWrapper(spanData));
      }

      return exportedSpans;
    }

    @Override
    public void dispose() {
      muleOtlpGrpcSpanExporter.dispose(this);
    }

    private static final class SpanDataWrapper implements CapturedExportedSpan {

      public static final String EXCEPTION_EVENT_NAME = "exception";
      private final SpanData spanData;

      public SpanDataWrapper(SpanData spanData) {
        this.spanData = spanData;
      }

      @Override
      public String getName() {
        return spanData.getName();
      }

      @Override
      public String getParentSpanId() {
        return spanData.getParentSpanId();
      }

      @Override
      public String getSpanId() {
        return spanData.getSpanId();
      }

      @Override
      public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<>();
        spanData.getAttributes().asMap().forEach((k, v) -> attributes.put(k.getKey(), valueOf(v)));
        return attributes;
      }

      public List<CapturedEventData> getEvents() {
        return spanData.getEvents().stream().map(
                                                 OpenTelemetryEventDataWrapper::new)
            .collect(Collectors.toList());
      }

      @Override
      public String getServiceName() {
        return spanData.getResource().getAttribute(OpenTelemetryResources.SERVICE_NAME_KEY);
      }

      @Override
      public String toString() {
        return String.format("a span with name: [%s], ID: [%s] and parent Span ID: [%s]", getName(), getSpanId(),
                             getParentSpanId());
      }

      @Override
      public boolean hasErrorStatus() {
        return spanData.getStatus().getStatusCode().equals(ERROR);
      }
    }

    /**
     * Allows capturing the Open Telemetry Span events.
     */
    private static final class OpenTelemetryEventDataWrapper implements CapturedEventData {

      private final EventData eventData;

      public OpenTelemetryEventDataWrapper(EventData eventData) {
        this.eventData = eventData;
      }

      @Override
      public String getName() {
        return eventData.getName();
      }

      @Override
      public Map<String, Object> getAttributes() {
        Map<String, Object> events = new HashMap<>();
        eventData.getAttributes().asMap()
            .forEach((attributeKey, attributeValue) -> events.put(attributeKey.getKey(), valueOf(attributeValue)));
        return events;
      }
    }
  }


}
