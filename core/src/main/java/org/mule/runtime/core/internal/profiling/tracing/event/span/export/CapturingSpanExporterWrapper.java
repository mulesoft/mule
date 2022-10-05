/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.SERVICE_NAME_KEY;

import static java.lang.String.valueOf;

import static io.opentelemetry.api.trace.StatusCode.ERROR;

import org.mule.runtime.core.privileged.profiling.CapturedEventData;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

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

/**
 * A {@link SpanExporter} that captures Open Telemetry exported spans.
 *
 * @since 4.5.0
 */
public class CapturingSpanExporterWrapper implements SpanExporter {

  private final SpanExporter delegate;
  private Set<MuleSpanCapturer> spanCapturers = ConcurrentHashMap.newKeySet();

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

  public ExportedSpanCapturer getSpanCapturer() {
    MuleSpanCapturer spanCapturer = new MuleSpanCapturer(this);
    spanCapturers.add(spanCapturer);
    return spanCapturer;
  }

  private void dispose(ExportedSpanCapturer muleSpanCapturer) {
    spanCapturers.remove(muleSpanCapturer);
  }

  private static final class MuleSpanCapturer implements ExportedSpanCapturer {

    private final CapturingSpanExporterWrapper muleOtlpGrpcSpanExporter;
    private Set<SpanData> spanData = ConcurrentHashMap.newKeySet();

    public MuleSpanCapturer(CapturingSpanExporterWrapper muleOtlpGrpcSpanExporter) {
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
        return spanData.getResource().getAttribute(SERVICE_NAME_KEY);
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
