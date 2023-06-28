/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.impl.config;

import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfigurator;
import org.mule.runtime.metrics.exporter.impl.optel.resources.grpc.GrpcMeterExporterConfigurator;
import org.mule.runtime.metrics.exporter.impl.optel.resources.http.HttpMeterExporterConfigurator;
import org.mule.runtime.metrics.exporter.impl.optel.resources.inmemory.InMemoryMeterExporterConfigurator;

/**
 * Specifies different ways in which metrics can be exported.
 *
 * @since 4.5.0
 */
public enum OpenTelemetryMeterExporterTransport {

  GRPC(new GrpcMeterExporterConfigurator()),

  HTTP(new HttpMeterExporterConfigurator()),

  IN_MEMORY(new InMemoryMeterExporterConfigurator());

  private final MeterExporterConfigurator meterExporterConfigurator;

  OpenTelemetryMeterExporterTransport(MeterExporterConfigurator meterExporterConfigurator) {
    this.meterExporterConfigurator = meterExporterConfigurator;
  }

  public MeterExporterConfigurator getMeterExporterConfigurator() {
    return meterExporterConfigurator;
  }
}
