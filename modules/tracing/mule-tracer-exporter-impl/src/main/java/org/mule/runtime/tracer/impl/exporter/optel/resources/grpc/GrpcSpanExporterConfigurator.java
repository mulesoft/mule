/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.resources.grpc;

import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_HEADERS;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.tracer.impl.exporter.config.SpanExporterConfigUtils.enableBackoffStrategy;

import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.optel.resources.SpanExporterConfigurator;
import org.mule.runtime.tracer.impl.exporter.optel.resources.SpanExporterConfiguratorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * A configurator for the span exporter configurator based on grpc.
 *
 * @since 4.5.0
 */
public class GrpcSpanExporterConfigurator implements SpanExporterConfigurator {

  @Override
  public SpanExporter configExporter(SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    String endpoint = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);

    // If we have an endpoint, we set it.
    OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder();
    if (!isEmpty(endpoint)) {
      builder.setEndpoint(endpoint);
    }

    // We verify if we have a compression type and set it.
    String type = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE);
    if (type != null) {
      builder.setCompression(type);
    }

    // If we must enable tls, we do it.
    if (parseBoolean(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "false"))) {
      configureTls(builder, spanExporterConfiguration);
    }

    // Headers
    String headers = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_HEADERS);
    if (headers != null) {
      configureHeaders(builder, headers);
    }

    // Timeout
    String timeout = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TIMEOUT);
    if (timeout != null) {
      configureTimeout(builder, timeout);
    }

    // Backoff strategy
    enableBackoffStrategy(builder, spanExporterConfiguration);

    return builder.build();
  }

  private void configureTimeout(OtlpGrpcSpanExporterBuilder builder, String timeout) {
    builder.setTimeout(parseLong(timeout), MILLISECONDS);
  }

  private void configureHeaders(OtlpGrpcSpanExporterBuilder builder, String headers) throws SpanExporterConfiguratorException {
    try {
      Map<String, String> retMap = new Gson().fromJson(
                                                       headers, new TypeToken<HashMap<String, Object>>() {}.getType());

      retMap.forEach(builder::addHeader);

    } catch (Exception e) {
      throw new SpanExporterConfiguratorException(e);
    }
  }

  private void configureTls(OtlpGrpcSpanExporterBuilder builder, SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    configureTrustedCertificates(builder, spanExporterConfiguration);
    configureClientTls(builder, spanExporterConfiguration);
  }

  private static void configureTrustedCertificates(OtlpGrpcSpanExporterBuilder builder,
                                                   SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {

    String caFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION);

    if (caFilePath != null) {
      try {
        byte[] caFileBytes = readAllBytes(get(caFilePath));
        builder.setTrustedCertificates(caFileBytes);
      } catch (IOException e) {
        throw new SpanExporterConfiguratorException(e);
      }
    }
  }

  private void configureClientTls(OtlpGrpcSpanExporterBuilder builder, SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    String certFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION);
    String keyFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);

    if (certFilePath != null && keyFilePath != null) {
      try {
        byte[] keyFileBytes = readAllBytes(get(keyFilePath));
        byte[] certFileBytes = readAllBytes(get(certFilePath));
        builder.setClientTls(keyFileBytes, certFileBytes);
      } catch (IOException e) {
        throw new SpanExporterConfiguratorException(e);
      }
    }
  }
}
