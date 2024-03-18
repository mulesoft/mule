/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
