/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;

public class TestOpenTelemetryMeterExporterUtils {

  public static final String SERVICE_NAME_KEY = "service.name";

  /**
   * Returns the exported metrics from the protobuf representation.
   *
   * @param request the protobuf representation from the sdk.
   *
   * @return the list of retrieved meters.
   */
  public static List<TestExportedMeter> getMetrics(ExportMetricsServiceRequest request) {
    List<TestExportedMeter> exportedMeters = new ArrayList<>();

    for (ResourceMetrics resourceMetrics : request.getResourceMetricsList()) {
      // Adding the resource name.
      List<KeyValue> attributeKeyValues = resourceMetrics.getResource().getAttributesList();

      for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetricsList()) {
        for (Metric metrics : scopeMetrics.getMetricsList()) {
          TestExportedMeter exportedMeter = new TestExportedMeter();
          addResourceName(exportedMeter, attributeKeyValues);
          exportedMeter.setDescription(metrics.getDescription());
          exportedMeter.setInstrumentName(scopeMetrics.getScope().getName());
          exportedMeter.setName(metrics.getName());
          exportedMeter.setValue(metrics.getSum().getDataPoints(0).getAsInt());
          exportedMeters.add(exportedMeter);
        }
      }
    }

    return exportedMeters;
  }

  private static void addResourceName(TestExportedMeter exportedMeter, List<KeyValue> attributeKeyValues) {
    for (KeyValue attributeKeyValue : attributeKeyValues) {
      if (attributeKeyValue.getKey().equals(SERVICE_NAME_KEY)) {
        exportedMeter.setResourceName(attributeKeyValue.getValue().getStringValue());
      }
    }
  }
}
