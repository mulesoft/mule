/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest.parseFrom;

import org.mule.tck.junit4.rule.DynamicPort;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.ServerRule;

import org.jetbrains.annotations.NotNull;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;

public class TestServerRule extends ServerRule {

  private final DynamicPort port;

  public TestServerRule(DynamicPort port) {
    this.port = port;
  }

  private static final String GRPC_ENDPOINT_PATH = "/opentelemetry.proto.collector.metrics.v1.MetricsService/Export";

  private List<TestExportedMeter> metrics = new ArrayList<>();

  @Override
  protected void configure(ServerBuilder sb) {
    sb.service(GRPC_ENDPOINT_PATH,
               new AbstractUnaryGrpcService() {

                 @Override
                 protected @NotNull CompletionStage<byte[]> handleMessage(@NotNull ServiceRequestContext ctx,
                                                                          byte @NotNull [] message) {
                   try {
                     metrics = TestOpenTelemetryMeterExporterUtils.getMetrics(parseFrom(message));
                   } catch (InvalidProtocolBufferException e) {
                     throw new UncheckedIOException(e);
                   }
                   return completedFuture(ExportMetricsServiceResponse.getDefaultInstance().toByteArray());
                 }
               });

    sb.http(port.getNumber());
  }

  public void reset() {
    metrics.clear();
  }

  public List<TestExportedMeter> getMetrics() {
    return metrics;
  }
}
