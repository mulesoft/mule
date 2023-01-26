/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.parseBoolean;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.builder;
import static org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources.getNewExportedSpanSniffer;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanExporterManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import javax.inject.Inject;

import io.opentelemetry.sdk.trace.SpanProcessor;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

/**
 * An implementation of {@link SpanExporterFactory} that creates {@link SpanExporter} that exports the internal spans as
 * {@link OpenTelemetrySpanExporter}
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory, Disposable {

  @Inject
  SpanExporterConfiguration configuration;

  @Inject
  MuleContext muleContext;

  private SpanProcessor spanProcessor;

  private final boolean isExportEnabled = parseBoolean(configuration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, FALSE.toString()));

  private static final io.opentelemetry.sdk.trace.export.SpanExporter DUMMY_EXPORTER = new OpenTelemetryResources.DummySpanExporter();

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, InitialSpanInfo initialExportInfo) {
    return builder()
        .withStartSpanInfo(initialExportInfo)
        .withArtifactId(muleContext.getConfiguration().getId())
        .withArtifactType(muleContext.getArtifactType().getAsString())
        .withSpanProcessor(resolveOpenTelemetrySpanProcessor())
        .withInternalSpan(internalSpan)
        .build();
  }

  private SpanProcessor resolveOpenTelemetrySpanProcessor() {
    if (spanProcessor == null) {
      if (feat)
      spanProcessor = OpenTelemetryResources.resolveOpenTelemetrySpanProcessor(configuration, resolveOpenTelemetrySpanExporter());
    }
    return spanProcessor;
  }

  protected io.opentelemetry.sdk.trace.export.SpanExporter resolveOpenTelemetrySpanExporter() {
    if (!parseBoolean(configuration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, FALSE.toString()))) {
      return DUMMY_EXPORTER;
    }
    return OpenTelemetryResources.resolveOpenTelemetrySpanExporter(configuration);
  }

  @Override
  public SpanExporterManager getSpanExporterManager() {
    return new OpenTelemetrySpanExporterManager();
  }

  @Override
  public void dispose() {
    spanProcessor.shutdown();
  }

  private static class OpenTelemetrySpanExporterManager implements SpanExporterManager {

    @Override
    public ExportedSpanSniffer getExportedSpanSniffer() {
      return getNewExportedSpanSniffer();
    }
  }
}
