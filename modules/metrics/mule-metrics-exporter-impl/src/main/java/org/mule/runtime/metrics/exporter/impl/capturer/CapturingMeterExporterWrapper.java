/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.impl.capturer;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;

public class CapturingMeterExporterWrapper implements MetricExporter {

  private final Set<InMemoryMetricExporter> meterSniffers = ConcurrentHashMap.newKeySet();

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    meterSniffers.forEach(sniffer -> sniffer.export(metrics));
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return null;
  }

  @Override
  public CompletableResultCode shutdown() {
    return null;
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }

  public InMemoryMetricExporter getExportedMeterSniffer() {
    InMemoryMetricExporter meterSniffer = InMemoryMetricExporter.create();
    meterSniffers.add(meterSniffer);
    return meterSniffer;
  }

  public void dispose(InMemoryMetricExporter meterSniffer) {
    meterSniffers.remove(meterSniffer);
  }
}
