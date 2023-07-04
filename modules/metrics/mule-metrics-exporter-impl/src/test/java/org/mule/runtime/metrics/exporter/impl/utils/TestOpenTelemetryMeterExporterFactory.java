/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
