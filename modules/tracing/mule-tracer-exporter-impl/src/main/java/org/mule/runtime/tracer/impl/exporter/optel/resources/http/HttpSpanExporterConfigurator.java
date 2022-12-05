/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.resources.http;

import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;

import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.optel.resources.SpanExporterConfigurator;
import org.mule.runtime.tracer.impl.exporter.optel.resources.SpanExporterConfiguratorException;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * A {@link SpanExporterConfigurator} for http export.
 *
 * @since 4.5.0
 */
public class HttpSpanExporterConfigurator implements SpanExporterConfigurator {

  @Override
  public SpanExporter configExporter(SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    String endpoint = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);

    // If we have an endpoint, we set it.
    OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder();
    if (!StringUtils.isEmpty(endpoint)) {
      builder.setEndpoint(endpoint);
    }

    // We verify if we have a compression type and set it.
    String type = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE);
    if (type != null) {
      builder.setCompression(type);
    }

    // If we must enable tls, we do it.
    if (spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "false").equals(TRUE.toString())) {
      configureTls(builder, spanExporterConfiguration);
    }
    return builder.build();
  }

  private void configureTls(OtlpHttpSpanExporterBuilder builder, SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    configureTrustedCertificates(builder, spanExporterConfiguration);
    configureClientTls(builder, spanExporterConfiguration);
  }

  private static void configureTrustedCertificates(OtlpHttpSpanExporterBuilder builder,
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

  private void configureClientTls(OtlpHttpSpanExporterBuilder builder, SpanExporterConfiguration spanExporterConfiguration)
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
