/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl.optel.resources.inmemory;

import static org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory.METER_SNIFFER_EXPORTER;

import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfigurator;
import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfiguratorException;

import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class InMemoryMeterExporterConfigurator implements MeterExporterConfigurator {

  @Override
  public MetricExporter configExporter(MeterExporterConfiguration meterExporterConfiguration)
      throws MeterExporterConfiguratorException {
    return METER_SNIFFER_EXPORTER;
  }

}
