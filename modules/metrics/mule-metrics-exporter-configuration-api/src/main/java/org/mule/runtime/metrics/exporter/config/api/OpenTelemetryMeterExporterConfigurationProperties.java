/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.config.api;

public class OpenTelemetryMeterExporterConfigurationProperties {

  private static final String MULE_OPEN_TELEMETRY_METER_EXPORTER = "mule.openTelemetry.meter.exporter";

  private OpenTelemetryMeterExporterConfigurationProperties() {}

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".enabled";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".configurationFilePath";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".type";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_TLS_ENABLED = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".tls.enabled";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_TIMEOUT = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".timeout";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".endpoint";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_COMPRESSION_TYPE =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".compression";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".tls.caFileLocation";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_CERT_FILE_LOCATION =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".tls.certFileLocation";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".tls.keyFileLocation";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_HEADERS = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".headers";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_AGGREGATION_TEMPORALITY =
      MULE_OPEN_TELEMETRY_METER_EXPORTER + ".aggregationTemporality";

  public static final String MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL = MULE_OPEN_TELEMETRY_METER_EXPORTER + ".interval";
}
