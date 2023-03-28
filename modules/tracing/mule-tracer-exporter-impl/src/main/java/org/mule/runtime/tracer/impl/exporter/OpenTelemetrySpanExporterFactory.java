/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetrySpanExporter.builder;
import static java.lang.Boolean.parseBoolean;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import org.mule.runtime.tracer.impl.exporter.capturer.CapturingSpanExporterWrapper;
import org.mule.runtime.tracer.impl.exporter.optel.config.OpenTelemetryAutoConfigurableSpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;

import javax.inject.Inject;

import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * An implementation of {@link SpanExporterFactory} that creates {@link SpanExporter} that exports the internal spans as
 * {@link OpenTelemetrySpanExporter}
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory, Disposable {

  @Inject
  private SpanExporterConfiguration configuration;

  private SpanExporterConfiguration privilegedConfiguration =
      new OpenTelemetryAutoConfigurableSpanExporterConfiguration(key -> null);

  private final LazyValue<SpanProcessor> spanProcessor = new LazyValue<>(this::resolveOpenTelemetrySpanProcessor);

  private static final CapturingSpanExporterWrapper SNIFFED_EXPORTER =
      new CapturingSpanExporterWrapper(new OpenTelemetryResources.NoOpSpanExporter());

  public OpenTelemetrySpanExporterFactory() {}

  /**
   * Creates a new {@link OpenTelemetrySpanExporterFactory} instance.
   * 
   * @param privilegedConfiguration Configuration that will be used to config export parameters that are not meant to be
   *                                configurable for the end users.
   */
  protected OpenTelemetrySpanExporterFactory(SpanExporterConfiguration privilegedConfiguration) {
    this.privilegedConfiguration = privilegedConfiguration;
  }

  @Override
  public SpanExporter getSpanExporter(InternalSpan internalSpan, InitialSpanInfo initialExportInfo) {
    return builder()
        .withStartSpanInfo(initialExportInfo)
        .withSpanProcessor(spanProcessor.get())
        .withInternalSpan(internalSpan)
        .build();
  }

  protected SpanProcessor resolveOpenTelemetrySpanProcessor() {
    if (isExportEnabled()) {
      return OpenTelemetryResources.resolveOpenTelemetrySpanProcessor(configuration, privilegedConfiguration,
                                                                      resolveOpenTelemetrySpanExporter());
    } else {
      return SimpleSpanProcessor.create(SNIFFED_EXPORTER);
    }
  }

  private boolean isExportEnabled() {
    return parseBoolean(configuration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED));
  }

  protected io.opentelemetry.sdk.trace.export.SpanExporter resolveOpenTelemetrySpanExporter() {
    return OpenTelemetryResources.resolveOpenTelemetrySpanExporter(configuration);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return new OpenTelemetrySpanSnifferManager();
  }

  @Override
  public void dispose() {
    spanProcessor.get().shutdown();
  }

  private static class OpenTelemetrySpanSnifferManager implements SpanSnifferManager {

    @Override
    public ExportedSpanSniffer getExportedSpanSniffer() {
      return SNIFFED_EXPORTER.getExportedSpanSniffer();
    }
  }
}
