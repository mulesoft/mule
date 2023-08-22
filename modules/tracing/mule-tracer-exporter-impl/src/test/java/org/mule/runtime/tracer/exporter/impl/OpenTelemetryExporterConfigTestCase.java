/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_HEADERS;
import static org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources.getPropagator;
import static org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources.getResource;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.tracer.exporter.impl.config.type.OpenTelemetryExporterTransport.GRPC;
import static org.mule.runtime.tracer.exporter.impl.config.type.OpenTelemetryExporterTransport.HTTP;
import static org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources.resolveOpenTelemetrySpanExporter;
import static org.mule.runtime.tracer.exporter.impl.optel.resources.OpenTelemetryResources.resolveOpenTelemetrySpanProcessor;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.testcontainers.Testcontainers.exposeHostPorts;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.utility.MountableFile.forHostPath;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.optel.config.OpenTelemetryAutoConfigurableSpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.optel.resources.SpanExporterConfiguratorException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.SelfSignedCertificateRule;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetryExporterConfigTestCase {

  public static final String TEST_SERVICE_NAME = "test-service-name";

  private static final DockerImageName COLLECTOR_IMAGE =
      DockerImageName.parse("ghcr.io/open-telemetry/opentelemetry-java/otel-collector");

  private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
  private static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;
  private static final Integer COLLECTOR_OTLP_GRPC_MTLS_PORT = 5317;
  private static final Integer COLLECTOR_OTLP_HTTP_MTLS_PORT = 5318;
  private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;

  public static final int TIMEOUT_MILLIS = 30000;

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";
  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private GenericContainer<?> collector;

  @ClassRule
  public static SelfSignedCertificateRule serverTls = new SelfSignedCertificateRule();

  @ClassRule
  public static SelfSignedCertificateRule clientTls = new SelfSignedCertificateRule();

  @ClassRule
  public static final GrpcServerRule server = new GrpcServerRule();

  @Before
  public void before() {
    exposeHostPorts(server.httpPort());
    // Configuring the collector test-container
    collector =
        new GenericContainer<>(COLLECTOR_IMAGE)
            .withImagePullPolicy(PullPolicy.alwaysPull())
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
            .withEnv(
                     "OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + server.httpPort())
            .withClasspathResourceMapping(
                                          "otel.yaml", "/otel.yaml", READ_ONLY)
            .withCommand("--config", "/otel.yaml")
            .withExposedPorts(
                              COLLECTOR_OTLP_GRPC_PORT,
                              COLLECTOR_OTLP_HTTP_PORT,
                              COLLECTOR_OTLP_GRPC_MTLS_PORT,
                              COLLECTOR_OTLP_HTTP_MTLS_PORT,
                              COLLECTOR_HEALTH_CHECK_PORT)
            .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

    collector.start();
  }

  @After
  public void after() {
    server.reset();
  }

  @Test
  public void defaultGrpcExporter() {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT));

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @Test
  public void defaultHttpExporter() {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, HTTP.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_HTTP_PORT) + "/v1/traces");

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @Test
  public void configuredGrpcInsecureExporter() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, GRPC.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT));
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, "512");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE, "gzip");
    properties
        .put(MULE_OPEN_TELEMETRY_EXPORTER_HEADERS,
             "{\"Header\": \"Header Value\"}");

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @Test
  public void configuredGrpcSecureExporter() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, GRPC.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "https://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_MTLS_PORT));
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, "512");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "true");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION, clientTls.privateKeyFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION, clientTls.certificateFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION, serverTls.certificateFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_COMPRESSION_TYPE, "gzip");
    properties
        .put(MULE_OPEN_TELEMETRY_EXPORTER_HEADERS,
             "{\"Header\": \"Header Value\"}");

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> !server.getTraceRequests().isEmpty()
            && server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @Test
  public void configuredHttpInsecureExporter() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, HTTP.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_HTTP_PORT) + "/v1/traces");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, "512");
    properties
        .put(MULE_OPEN_TELEMETRY_EXPORTER_HEADERS,
             "{\"Header\": \"Header Value\"}");

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @Test
  public void configuredHttpSecureExporter() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, HTTP.toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                   "https://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_HTTP_MTLS_PORT)
                       + "/v1/traces");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE, "512");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "true");
    properties
        .put(MULE_OPEN_TELEMETRY_EXPORTER_HEADERS,
             "{\"Header\": \"Header Value\"}");
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION, clientTls.privateKeyFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION, clientTls.certificateFile().toPath().toString());
    properties.put(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION, serverTls.certificateFile().toPath().toString());

    exportSpan(properties);

    new PollingProber(TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL)
        .check(new JUnitLambdaProbe(() -> !server.getTraceRequests().isEmpty()
            && server.getTraceRequests().get(0).getResourceSpansCount() == 1));
  }

  @NotNull
  private static OpenTelemetryAutoConfigurableSpanExporterConfiguration getSpanExporterConfiguration(Map<String, String> properties) {
    return new OpenTelemetryAutoConfigurableSpanExporterConfiguration(new TestSpanExporterConfiguration(properties));
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
    public String getStringValue(String key) {
      return properties.get(key);
    }

  }
  private static class GrpcServerRule extends ServerRule {

    private final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();

    @Override
    protected void configure(ServerBuilder sb) throws Exception {
      sb.service(
                 "/opentelemetry.proto.collector.trace.v1.TraceService/Export",
                 new AbstractUnaryGrpcService() {

                   @Override
                   protected @NotNull CompletionStage<byte[]> handleMessage(
                                                                            @NotNull ServiceRequestContext ctx,
                                                                            byte @NotNull [] message) {
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

    public void reset() {
      traceRequests.clear();
    }

    public List<ExportTraceServiceRequest> getTraceRequests() {
      return traceRequests;
    }

  }

  private static Tracer getTracer(SpanExporterConfiguration spanExporterConfiguration, String serviceName)
      throws SpanExporterConfiguratorException {
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder()
        .addSpanProcessor(resolveOpenTelemetrySpanProcessor(spanExporterConfiguration,
                                                            spanExporterConfiguration,
                                                            resolveOpenTelemetrySpanExporter(spanExporterConfiguration)))
        .setResource(getResource(serviceName));

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProviderBuilder.build())
        .setPropagators(getPropagator())
        .build();

    return openTelemetry.getTracer(MULE_INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
  }

  private void exportSpan(Map<String, String> properties) {
    OpenTelemetryAutoConfigurableSpanExporterConfiguration spanExporterConfiguration = getSpanExporterConfiguration(properties);
    SpanProcessor spanProcessor = resolveOpenTelemetrySpanProcessor(spanExporterConfiguration, spanExporterConfiguration,
                                                                    resolveOpenTelemetrySpanExporter(spanExporterConfiguration));
    Tracer tracer = getTracer(getSpanExporterConfiguration(properties), TEST_SERVICE_NAME);
    ReadableSpan span = (ReadableSpan) tracer.spanBuilder(getUUID()).startSpan();

    spanProcessor.onEnd(span);
  }
}
