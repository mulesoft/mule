/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.config.type;

import org.mule.runtime.tracer.exporter.impl.optel.resources.SpanExporterConfigurator;
import org.mule.runtime.tracer.exporter.impl.optel.resources.grpc.GrpcSpanExporterConfigurator;
import org.mule.runtime.tracer.exporter.impl.optel.resources.http.HttpSpanExporterConfigurator;

/**
 * The transport of export of the spans.
 *
 * @since 4.5.0
 */
public enum OpenTelemetryExporterTransport {

  GRPC(new GrpcSpanExporterConfigurator()),

  HTTP(new HttpSpanExporterConfigurator());

  private final SpanExporterConfigurator spanExporterConfigurator;

  OpenTelemetryExporterTransport(SpanExporterConfigurator spanExporterConfigurator) {
    this.spanExporterConfigurator = spanExporterConfigurator;
  }

  public SpanExporterConfigurator getSpanExporterConfigurator() {
    return spanExporterConfigurator;
  }
}
