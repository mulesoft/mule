/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.api;

import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

/**
 * A factory for {@link MeterExporter}
 *
 * @since 4.5.0
 */
public interface MeterExporterFactory {

  /**
   * @param configuration the {@link MeterExporterConfiguration} with the details to create the exporter
   *
   * @return a {@link MeterExporter}.
   */
  MeterExporter getMeterExporter(MeterExporterConfiguration configuration);
}
