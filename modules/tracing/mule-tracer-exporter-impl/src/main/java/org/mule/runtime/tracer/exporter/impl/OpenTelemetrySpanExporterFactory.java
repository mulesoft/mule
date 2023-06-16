/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources.getResource;

import static java.lang.Boolean.parseBoolean;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.capturer.CapturingSpanExporterWrapper;
import org.mule.runtime.tracer.exporter.impl.optel.config.OpenTelemetryAutoConfigurableSpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources;

import javax.inject.Inject;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link SpanExporterFactory} that creates {@link SpanExporter} that exports the internal spans as
 * {@link OpenTelemetrySpanExporter}
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterFactory implements SpanExporterFactory, Disposable, Initialisable {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetrySpanExporterFactory.class);

  @Inject
  private SpanExporterConfiguration configuration;

  private SpanExporterConfiguration privilegedConfiguration =
      new OpenTelemetryAutoConfigurableSpanExporterConfiguration(key -> null);

  private static final CapturingSpanExporterWrapper SNIFFED_EXPORTER =
      new CapturingSpanExporterWrapper(new OpenTelemetryResources.NoOpSpanExporter());

  private FeatureFlaggingService featureFlaggingService;
  private MuleContext muleContext;
  private SpanProcessor spanProcessor;
  private Resource resource;
  private String artifactId;
  private String artifactType;
  private boolean addMuleAncestorSpanId;

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

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Inject
  public void setFeatureFlaggingService(FeatureFlaggingService featureFlaggingService) {
    this.featureFlaggingService = featureFlaggingService;
  }

  public SpanExporter getSpanExporter(InternalSpan internalSpan, InitialSpanInfo initialSpanInfo) {
    return new OpenTelemetrySpanExporter(internalSpan, initialSpanInfo, artifactId, artifactType, spanProcessor,
                                         addMuleAncestorSpanId, resource);
  }

  protected SpanProcessor resolveOpenTelemetrySpanProcessor() {
    if (isExportEnabled()) {
      LOGGER.info("Mule Open Telemetry Tracer Exporter is enabled.");
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
  public void initialise() throws InitialisationException {
    this.artifactId = muleContext.getConfiguration().getId();
    this.artifactType = muleContext.getArtifactType().getAsString();
    this.resource = getResource(artifactId);
    this.spanProcessor = resolveOpenTelemetrySpanProcessor();
    this.addMuleAncestorSpanId = featureFlaggingService.isEnabled(ADD_MULE_SPECIFIC_TRACING_INFORMATION_IN_TRACE_STATE);
    this.configuration.doOnConfigurationChanged(this::doOnConfigurationChanged);
  }

  private void doOnConfigurationChanged() {
    SpanProcessor previousSpanProcessor = spanProcessor;
    spanProcessor = resolveOpenTelemetrySpanProcessor();
    silentlyShutdown(previousSpanProcessor);
  }

  private static void silentlyShutdown(SpanProcessor previousSpanProcessor) {
    try {
      previousSpanProcessor.shutdown();
    } catch (Throwable e) {
      LOGGER.warn("Error in disposing span processor", e);
    }
  }

  @Override
  public void dispose() {
    silentlyShutdown(spanProcessor);
  }

  private static class OpenTelemetrySpanSnifferManager implements SpanSnifferManager {

    @Override
    public ExportedSpanSniffer getExportedSpanSniffer() {
      return SNIFFED_EXPORTER.getExportedSpanSniffer();
    }
  }
}
