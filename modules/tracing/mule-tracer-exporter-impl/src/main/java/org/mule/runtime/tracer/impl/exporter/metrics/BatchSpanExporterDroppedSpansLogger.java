package org.mule.runtime.tracer.impl.exporter.metrics;

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

import java.util.Collection;

public class BatchSpanExporterDroppedSpansLogger implements MetricExporter {

  private static final Logger METRICS_LOGGER = LoggerFactory.getLogger(BatchSpanExporterDroppedSpansLogger.class);
  public static final String PROCESSED_SPANS = "processedSpans";

  private Long loggedDroppedSpans = 0L;

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    metrics.forEach(metricData -> {
      if (metricData.getName().equals(PROCESSED_SPANS)) {
        evaluateDroppedSpans(metricData);
      }
    });
    return CompletableResultCode.ofSuccess();
  }

  private void evaluateDroppedSpans(MetricData metricData) {
    PointData pointData = metricData.getData().getPoints().iterator().next();
    if (pointData.getAttributes().get(AttributeKey.booleanKey("dropped"))) {
      logIfRelevant(((LongPointData) pointData).getValue());
    }
  }

  private void logIfRelevant(long currentDroppedSpans) {
    if (currentDroppedSpans > loggedDroppedSpans) {
      METRICS_LOGGER.warn("Export queue overflow: {} spans has been dropped. Total spans dropped since exporter start: {}",
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
    return AggregationTemporality.CUMULATIVE;
  }

}
