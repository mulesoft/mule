/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.getTracer;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.tracer.impl.exporter.config.type.OpenTelemetryExporterType.GRPC;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.DEFAULT_TIMEOUT;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.utility.MountableFile.forHostPath;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.testing.junit4.server.SelfSignedCertificateRule;
import org.junit.Before;
import org.junit.ClassRule;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.trace.Tracer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

public class OpenTelemetryResourcesTestCase {

  public static final String TEST_SERVICE_NAME = "test-service-name";

  private static final DockerImageName COLLECTOR_IMAGE =
      DockerImageName.parse("ghcr.io/open-telemetry/opentelemetry-java/otel-collector");

  private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
  private static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;
  private static final Integer COLLECTOR_OTLP_GRPC_MTLS_PORT = 5317;
  private static final Integer COLLECTOR_OTLP_HTTP_MTLS_PORT = 5318;
  private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;
  public static final String HTTP_SCHEMA = "http://";
  public static final int TIMEOUT_MILLIS = 30000;

  public static AtomicInteger totalSpansReceivedByCollector = new AtomicInteger();

  private GenericContainer<?> collector;

  @ClassRule
  public static SelfSignedCertificateRule serverTls = new SelfSignedCertificateRule();

  @ClassRule
  public static SelfSignedCertificateRule clientTls = new SelfSignedCertificateRule();

  @ClassRule
  public static final ServerRule server = new ServerRule() {

    @Override protected void configure(ServerBuilder sb) throws Exception {
      private final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();

      sb.service(
        "/opentelemetry.proto.collector.trace.v1.TraceService/Export",
        new AbstractUnaryGrpcService() {
          @Override
          protected CompletionStage<byte[]> handleMessage(
            ServiceRequestContext ctx, byte[] message) {
            try {
              traceRequests.add(ExportTraceServiceRequest.parseFrom(message));
            } catch (InvalidProtocolBufferException e) {
              throw new UncheckedIOException(e);
            }
            return completedFuture(ExportTraceServiceResponse.getDefaultInstance().toByteArray());
          }
        });

      sb.http(0);
    }
  };

  @Before
  public void before() {

    // Configuring the collector test-container
    collector =
        new GenericContainer<>(COLLECTOR_IMAGE)
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withEnv("LOGGING_EXPORTER_LOG_LEVEL", "INFO")
            .withCopyFileToContainer(
                                     forHostPath(serverTls.certificateFile().toPath(), 365),
                                     "/server.cert")
            .withCopyFileToContainer(
                                     forHostPath(serverTls.privateKeyFile().toPath(), 365), "/server.key")
            .withCopyFileToContainer(
                                     forHostPath(clientTls.certificateFile().toPath(), 365),
                                     "/client.cert")
            .withEnv("MTLS_CLIENT_CERTIFICATE", "/client.cert")
            .withEnv("MTLS_SERVER_CERTIFICATE", "/server.cert")
            .withEnv("MTLS_SERVER_KEY", "/server.key")
            .withClasspathResourceMapping(
                                          "otel.yaml", "/otel.yaml", READ_ONLY)
            .withCommand("--config", "/otel.yaml")
            .withLogConsumer(this::consumeLoggerFrame)
            .withExposedPorts(
                              COLLECTOR_OTLP_GRPC_PORT,
                              COLLECTOR_OTLP_HTTP_PORT,
                              COLLECTOR_OTLP_GRPC_MTLS_PORT,
                              COLLECTOR_OTLP_HTTP_MTLS_PORT,
                              COLLECTOR_HEALTH_CHECK_PORT)
            .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

    collector.start();
  }

  private void consumeLoggerFrame(OutputFrame outputFrame) {
    try {
      String logline = outputFrame.getUtf8String();
      String spanExportPrefix = "TracesExporter\t";
      int start = logline.indexOf(spanExportPrefix);
      int end = logline.indexOf("}");
      if (start > 0) {
        String substring = logline.substring(start + spanExportPrefix.length(), end + 1);
        HashMap<?, ?> mapping = new ObjectMapper().readValue(substring, HashMap.class);
        totalSpansReceivedByCollector.addAndGet((Integer) mapping.get("#spans"));
      }
    } catch (JsonProcessingException e) {
      // Nothing to do.
    }
  }

  @Test
  public void defaultGrpcInsecureExporter() {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, GRPC.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT));
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE, "0");
    Tracer tracer = getTracer(new TestSpanExporterConfiguration(properties), TEST_SERVICE_NAME);

    tracer.spanBuilder(getUUID()).startSpan().end();
    new PollingProber(DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(totalSpansReceivedByCollector.get(), is(1));
      return true;
    }));
  }

  @Test
  public void defaultGrpcSecureExporter() {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, GRPC.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "https://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_MTLS_PORT));
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE, "0");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "true");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION, clientTls.privateKeyFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION, clientTls.certificateFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION, serverTls.certificateFile().toPath().toString());

    Tracer tracer = getTracer(new TestSpanExporterConfiguration(properties), TEST_SERVICE_NAME);

    tracer.spanBuilder(getUUID()).startSpan().end();
    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(totalSpansReceivedByCollector.get(), is(1));
      return true;
    }));
  }


  /**
   * A {@link SpanExporterConfiguration} for testing.
   */
  private static class TestSpanExporterConfiguration implements SpanExporterConfiguration {

    private final Map<String, String> properties;

    public TestSpanExporterConfiguration(Map<String, String> properties) {
      this.properties = properties;
    }

    @Override
    public String getValue(String key) {
      return properties.get(key);
    }
  }
}
