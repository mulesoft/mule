/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.metrics;

import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE;

import java.util.Collection;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metric exporter that will log information about the Open Telemetry export queue (currently backed by a
 * {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor}. Information about dropped spans will be logged as part of the
 * application logs in order to facilitate troubleshooting.
 *
 * @since 4.5.1
 *
 * @see io.opentelemetry.sdk.trace.export.BatchSpanProcessor
 */
public class OpenTelemetryExportQueueMetrics implements MetricExporter {

  private static final Logger METRICS_LOGGER = LoggerFactory.getLogger(OpenTelemetryExportQueueMetrics.class);
  public static final String PROCESSED_SPANS = "processedSpans";

  private Long loggedDroppedSpans = 0L;

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    metrics.forEach(metricData -> {
      if (metricData.getName().equals(PROCESSED_SPANS)) {
        checkForDroppedSpans(metricData);
      }
    });
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Logs a warning message if the amount of dropped spans has increased since the last call to this method.
   *
   * @param metricData Metric data about the dropped spans.
   */
  private void checkForDroppedSpans(MetricData metricData) {
    logIfRelevant(getDroppedSpans(metricData));
  }

  /**
   * @param processesSpansMetricData Processed spans metric data.
   * @return The total amount of dropped spans.
   */
  private long getDroppedSpans(MetricData processesSpansMetricData) {
    PointData pointData = processesSpansMetricData.getData().getPoints().iterator().next();
    if (Boolean.TRUE.equals(pointData.getAttributes().get(AttributeKey.booleanKey("dropped")))) {
      return ((LongPointData) pointData).getValue();
    } else {
      return 0L;
    }
  }

  /**
   * Logs a warning message if the amount of dropped spans has increased since the last call to this method.
   *
   * @param currentDroppedSpans Current amount of dropped spans.
   */
  private void logIfRelevant(long currentDroppedSpans) {
    if (currentDroppedSpans > loggedDroppedSpans) {
      METRICS_LOGGER.warn("Export queue overflow: {} spans have been dropped. Total spans dropped since the export started: {}",
                          currentDroppedSpans - loggedDroppedSpans, currentDroppedSpans);
      loggedDroppedSpans = currentDroppedSpans;
    }
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return CUMULATIVE;
  }

}
