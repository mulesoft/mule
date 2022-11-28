/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.builder;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import javax.inject.Inject;

public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory {

  @Inject
  MuleContext muleContext;

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, StartSpanInfo startSpanInfo) {
    return builder()
        .withStartSpanInfo(startSpanInfo)
        .withArtifactId(muleContext.getConfiguration().getId())
        .withArtifactType(muleContext.getArtifactType().getAsString())
        .withInternalSpan(internalSpan)
        .build();
  }

  @Override
  public SpanSnifferManager getSpanExporterManager() {
    return new OpenTelemetrySpanExporterManager();
  }

  private static class OpenTelemetrySpanExporterManager implements SpanSnifferManager {

    @Override
    public ExportedSpanSniffer getExportedSpanSniffer() {
      return OpenTelemetryResources.getNewExportedSpanCapturer();
    }
  }
}
