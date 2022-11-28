/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.config;

/**
 * Properties for Span Configuration Exporter.
 *
 * @since 4.5.0
 */
public class OpenTelemetrySpanExporterConfigurationProperties {

  private OpenTelemetrySpanExporterConfigurationProperties() {}

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_ENABLED = "mule.open.telemetry.exporter.enabled";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_TYPE = "mule.open.telemetry.exporter.type";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED = "mule.open.telemetry.exporter.tls.enabled";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION = "mule.open.telemetry.exporter.cert.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION = "mule.open.telemetry.exporter.key.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION = "mule.open.telemetry.exporter.ca.file.location";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE = "mule.open.telemetry.exporter.compression.type";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BALANCER_NAME = "mule.open.telemetry.exporter.balancer.name";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_HEADER = "mule.open.telemetry.exporter.header";

  public static final String MULE_OPEN_TELEMETRY_READ_BUFFER_SIZE = "mule.open.telemetry.read.buffer.size";

  public static final String MULE_OPEN_TELEMETRY_WRITE_BUFFER_SIZE = "mule.open.telemetry.write.buffer.size";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT = "mule.open.telemetry.exporter.endpoint";

  public static final String MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE = "mule.open.telemetry.exporter.batch.size";
}
