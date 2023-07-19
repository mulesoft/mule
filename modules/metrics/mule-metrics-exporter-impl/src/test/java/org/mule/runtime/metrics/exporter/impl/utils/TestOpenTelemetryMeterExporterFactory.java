/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

import org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory;

/**
 * A {@link OpenTelemetryMeterExporterFactory} for testing.
 */
public class TestOpenTelemetryMeterExporterFactory extends OpenTelemetryMeterExporterFactory {

  public static final String RESOURCE_ID = "app";

  @Override
  protected String getResourceId() {
    return RESOURCE_ID;
  }
}
