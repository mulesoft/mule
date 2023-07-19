/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.optel.resources;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * An exporter opentelemetry api configurator.
 *
 * @since 4.5.0
 */
public interface SpanExporterConfigurator {

  /**
   * Configs the exporter according to the @param spanExporterConfiguration and returns the {@link SpanExporter}
   *
   * @param spanExporterConfiguration the configuration.
   *
   * @return the {@link SpanExporter}
   *
   * @throws SpanExporterConfiguratorException the exception raised..
   */
  SpanExporter configExporter(SpanExporterConfiguration spanExporterConfiguration) throws SpanExporterConfiguratorException;
}
