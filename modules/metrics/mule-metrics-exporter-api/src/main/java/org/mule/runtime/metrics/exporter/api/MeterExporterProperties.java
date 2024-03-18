/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
