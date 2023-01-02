/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.builder;
import static org.mule.runtime.tracer.impl.exporter.config.SpanExporterConfigurationDiscoverer.discoverSpanExporterConfiguration;

import io.opentelemetry.sdk.trace.SpanProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.EnrichedInitialSpanInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

import javax.inject.Inject;

public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory {

  private static final SpanExporterConfiguration CONFIGURATION = discoverSpanExporterConfiguration();

  @Inject
  MuleContext muleContext;

  private SpanProcessor spanProcessor;

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, EnrichedInitialSpanInfo initialExportInfo) {
    return builder()
        .withStartSpanInfo(initialExportInfo)
        .withArtifactId(muleContext.getConfiguration().getId())
        .withArtifactType(muleContext.getArtifactType().getAsString())
        .withSpanProcessor(getSpanProcessor())
        .withInternalSpan(internalSpan)
        .build();
  }

  private SpanProcessor getSpanProcessor() {
    if (spanProcessor == null) {
      spanProcessor = OpenTelemetryResources.resolveExporterProcessor(CONFIGURATION);
    }
    return spanProcessor;
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
