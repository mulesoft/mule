/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
