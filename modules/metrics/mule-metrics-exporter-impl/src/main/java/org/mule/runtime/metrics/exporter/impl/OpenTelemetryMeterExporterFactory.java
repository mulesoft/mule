/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import org.mule.runtime.metrics.exporter.api.DummyConfiguration;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.api.MeterExporterFactory;
import org.mule.runtime.metrics.exporter.impl.capturer.CapturingMeterExporterWrapper;

public class OpenTelemetryMeterExporterFactory implements MeterExporterFactory {

  public static final CapturingMeterExporterWrapper METER_SNIFFER_EXPORTER = new CapturingMeterExporterWrapper();

  @Override
  public MeterExporter getMeterExporter(DummyConfiguration configuration) {
    return new OpenTelemetryMeterExporter(configuration);
  }
}
