/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl.utils;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;
import static io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest.parseFrom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.zip.GZIPInputStream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.ServerRule;

import org.jetbrains.annotations.NotNull;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;

public class TestServerRule extends ServerRule {

  public static final String GRPC_ENDPOINT_PATH = "/opentelemetry.proto.collector.metrics.v1.MetricsService/Export";
  public static final String HTTP_ENDPOINT_PATH = "/http";

  public static final String HTTP_GZIP_ENDPOINT_PATH = "/http_gzip";

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


    sb.service(HTTP_ENDPOINT_PATH,
               new AbstractHttpService() {

                 @Override
                 protected @NotNull HttpResponse doPost(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                   return HttpResponse.from(req.aggregate().handle((aReq, cause) -> {
                     CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
                     HttpResponse res = from(responseFuture);
                     try {
                       metrics = TestOpenTelemetryMeterExporterUtils.getMetrics(parseFrom(aReq.content().array()));
                     } catch (IOException e) {
                       // Nothing to do.
                     }
                     responseFuture.complete(HttpResponse.of(OK));
                     return res;
                   }));
                 }
               });

    sb.service(HTTP_GZIP_ENDPOINT_PATH,
               new AbstractHttpService() {

                 @Override
                 protected @NotNull HttpResponse doPost(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                   return HttpResponse.from(req.aggregate().handle((aReq, cause) -> {
                     CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
                     HttpResponse res = from(responseFuture);
                     try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(aReq.content().array()));
                         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                       byte[] buffer = new byte[1024];
                       int len;
                       while ((len = gzipInputStream.read(buffer)) != -1) {
                         byteArrayOutputStream.write(buffer, 0, len);
                       }
                       byte[] decompressedData = byteArrayOutputStream.toByteArray();
                       metrics = TestOpenTelemetryMeterExporterUtils.getMetrics(parseFrom(decompressedData));
                     } catch (IOException e) {
                       // Nothing to do.
                     }
                     responseFuture.complete(HttpResponse.of(OK));
                     return res;
                   }));
                 }
               });


    sb.http(0);

  }

  public void reset() {
    metrics.clear();
  }

  public List<TestExportedMeter> getMetrics() {
    return metrics;
  }

}
