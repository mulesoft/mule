/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * A {@link SpanExporter} that captures the exported spans.
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
        exportedSpans.add(new SpanDataWrapper(spanData, this));
      }

      return exportedSpans;
    }

    @Override
    public void dispose() {
      muleOtlpGrpcSpanExporter.dispose(this);
    }

    private static final class SpanDataWrapper implements CapturedExportedSpan {

      private final SpanData spanData;
      private final ExportedSpanCapturer exportedSpanCapturer;

      private SpanDataWrapper(SpanData spanData, ExportedSpanCapturer exportedSpanCapturer) {
        this.exportedSpanCapturer = exportedSpanCapturer;
        this.spanData = spanData;
      }

      @Override
      public String getName() {
        return spanData.getName();
      }

      @Override
      public Optional<CapturedExportedSpan> getParent() {
        return exportedSpanCapturer
            .getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getSpanId().equals(spanData.getParentSpanId()))
            .findFirst();
      }

      @Override
      public List<CapturedExportedSpan> getChildren() {
        return exportedSpanCapturer.getExportedSpans().stream()
            .filter(exportedSpan -> exportedSpan.getParent().map(parentSpan -> parentSpan.getSpanId()).orElse(null)
                .equals(spanData.getSpanId()))
            .collect(toList());
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
    }
  }


}
