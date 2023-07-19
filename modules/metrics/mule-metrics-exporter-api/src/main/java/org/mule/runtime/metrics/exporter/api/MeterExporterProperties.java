/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.api;

/**
 * Properties for meter exporter.
 *
 * @since 4.5.0
 */
public class MeterExporterProperties {

  /**
   * Determines if the exporter is enabled or not.
   */
  public static final String METRIC_EXPORTER_ENABLED_PROPERTY = "mule.open.telemetry.metric.exporter";

}
