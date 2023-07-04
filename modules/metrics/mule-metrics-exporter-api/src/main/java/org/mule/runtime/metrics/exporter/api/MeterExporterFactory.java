/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
