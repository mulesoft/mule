/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl.optel.resources.grpc;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_COMPRESSION_TYPE;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_HEADERS;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_TIMEOUT;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_TLS_ENABLED;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfigurator;
import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfiguratorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * A configurator for the span exporter configurator based on grpc.
 *
 * @since 4.5.0
 */
public class GrpcMeterExporterConfigurator implements MeterExporterConfigurator {

  @Override
  public MetricExporter configExporter(MeterExporterConfiguration meterExporterConfiguration)
      throws MeterExporterConfiguratorException {
    String endpoint = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT);

    // If we have an endpoint, we set it.
    OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder();
    if (!isEmpty(endpoint)) {
      builder.setEndpoint(endpoint);
    }

    // We verify if we have a compression type and set it.
    String type = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_COMPRESSION_TYPE);
    if (type != null) {
      builder.setCompression(type);
    }

    // If we must enable tls, we do it.
    if (parseBoolean(meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_TLS_ENABLED, "false"))) {
      configureTls(builder, meterExporterConfiguration);
    }

    // Headers
    String headers = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_HEADERS);
    if (headers != null) {
      configureHeaders(builder, headers);
    }

    // Timeout
    String timeout = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_TIMEOUT);
    if (timeout != null) {
      configureTimeout(builder, timeout);
    }

    return builder.build();
  }

  private void configureTimeout(OtlpGrpcMetricExporterBuilder builder, String timeout) {
    builder.setTimeout(parseLong(timeout), MILLISECONDS);
  }

  private void configureHeaders(OtlpGrpcMetricExporterBuilder builder, String headers) throws MeterExporterConfiguratorException {
    try {
      Map<String, String> retMap = new Gson().fromJson(
                                                       headers, new TypeToken<HashMap<String, Object>>() {}.getType());

      retMap.forEach(builder::addHeader);

    } catch (Exception e) {
      throw new MeterExporterConfiguratorException(e);
    }
  }

  private void configureTls(OtlpGrpcMetricExporterBuilder builder, MeterExporterConfiguration meterExporterConfiguration)
      throws MeterExporterConfiguratorException {
    configureTrustedCertificates(builder, meterExporterConfiguration);
    configureClientTls(builder, meterExporterConfiguration);
  }

  private static void configureTrustedCertificates(OtlpGrpcMetricExporterBuilder builder,
                                                   MeterExporterConfiguration meterExporterConfiguration)
      throws MeterExporterConfiguratorException {

    String caFilePath = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_CA_FILE_LOCATION);

    if (caFilePath != null) {
      try {
        byte[] caFileBytes = readAllBytes(get(caFilePath));
        builder.setTrustedCertificates(caFileBytes);
      } catch (IOException e) {
        throw new MeterExporterConfiguratorException(e);
      }
    }
  }

  private void configureClientTls(OtlpGrpcMetricExporterBuilder builder, MeterExporterConfiguration meterExporterConfiguration)
      throws MeterExporterConfiguratorException {
    String certFilePath = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_CERT_FILE_LOCATION);
    String keyFilePath = meterExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_KEY_FILE_LOCATION);

    if (certFilePath != null && keyFilePath != null) {
      try {
        byte[] keyFileBytes = readAllBytes(get(keyFilePath));
        byte[] certFileBytes = readAllBytes(get(certFilePath));
        builder.setClientTls(keyFileBytes, certFileBytes);
      } catch (IOException e) {
        throw new MeterExporterConfiguratorException(e);
      }
    }
  }
}
