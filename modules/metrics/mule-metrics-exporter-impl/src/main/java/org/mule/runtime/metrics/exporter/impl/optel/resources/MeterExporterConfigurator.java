/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.optel.resources;

import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * An exporter opentelemetry api configurator.
 *
 * @since 4.5.0
 */
public interface MeterExporterConfigurator {

  /**
   * Configs the exporter according to the @param meterExporterConfiguration and returns the {@link MetricExporter}
   *
   * @param meterExporterConfiguration the configuration.
   *
   * @return the {@link MetricExporter}
   *
   * @throws MeterExporterConfiguratorException the exception raised..
   */
  MetricExporter configExporter(MeterExporterConfiguration meterExporterConfiguration) throws MeterExporterConfiguratorException;
}
