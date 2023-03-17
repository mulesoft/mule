/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api.config;

/**
 * Properties for Span Configuration Exporter.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterConfigurationProperties {

  private static final String MULE_OPEN_TELEMETRY_EXPORTER = "mule.open.telemetry.exporter";

  private OpenTelemetrySpanExporterConfigurationProperties() {}

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_ENABLED = MULE_OPEN_TELEMETRY_EXPORTER + ".enabled";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_TYPE = MULE_OPEN_TELEMETRY_EXPORTER + ".type";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED = MULE_OPEN_TELEMETRY_EXPORTER + ".tls.enabled";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION =
      MULE_OPEN_TELEMETRY_EXPORTER + ".cert.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION = MULE_OPEN_TELEMETRY_EXPORTER + ".key.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION = MULE_OPEN_TELEMETRY_EXPORTER + ".ca.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE = MULE_OPEN_TELEMETRY_EXPORTER + ".compression.type";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_HEADERS = MULE_OPEN_TELEMETRY_EXPORTER + ".headers";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT = MULE_OPEN_TELEMETRY_EXPORTER + ".timeout";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT = MULE_OPEN_TELEMETRY_EXPORTER + ".endpoint";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE = MULE_OPEN_TELEMETRY_EXPORTER + ".batch.max.size";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE =
      MULE_OPEN_TELEMETRY_EXPORTER + ".batch.queue.size";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY =
      MULE_OPEN_TELEMETRY_EXPORTER + ".batch.scheduled.delay";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MAX_ATTEMPTS =
      MULE_OPEN_TELEMETRY_EXPORTER + "backoff.max.attempts";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_INITIAL_BACKOFF =
      MULE_OPEN_TELEMETRY_EXPORTER + ".backoff.initial";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_MAX_BACKOFF = MULE_OPEN_TELEMETRY_EXPORTER + "backoff.max";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BACKOFF_MULTIPLIER =
      MULE_OPEN_TELEMETRY_EXPORTER + "backoff.multiplier";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY =
      MULE_OPEN_TELEMETRY_EXPORTER + "metrics.log.frequency";

}
