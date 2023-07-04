/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.impl.config;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Dummy configuration to use for tests
 */
// TODO W-13065409: Delete and replace tests with real configuration or mock of real configuration
public class DummyConfiguration implements MeterExporterConfiguration {

  @Override
  public String getStringValue(String key) {
    Map<String, String> defaultProperties = new HashMap<>();
    defaultProperties.put(MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE, "GRPC");
    defaultProperties.put(MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL, "1");
    return defaultProperties.get(key);
  }

  public String getArtifactId(MuleContext muleContext) {
    return muleContext.getConfiguration().getId();
  }
}
