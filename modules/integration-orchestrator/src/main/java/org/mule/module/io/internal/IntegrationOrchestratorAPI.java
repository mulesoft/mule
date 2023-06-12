/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.io.internal;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.internal.listener.HttpRequestToResult;
import org.mule.extension.http.internal.listener.ListenerPath;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.serialization.ExtensionModelResolver;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.domain.message.response.HttpResponseBuilder;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.RequestHandler;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.http.api.server.async.ResponseStatusCallback;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class IntegrationOrchestratorAPI {

  private static Logger logger = LoggerFactory.getLogger(IntegrationOrchestratorAPI.class);

  private final Supplier<HttpService> httpServicexSupplier;
  private final MuleDeploymentService deploymentService;
  private SchedulerService schedulerService;

  private ExtensionModelResolver extensionModelResolver;

  public IntegrationOrchestratorAPI(Supplier<HttpService> httpServiceSupplier, DeploymentService deploymentService) {
    this.httpServicexSupplier = httpServiceSupplier;
    this.deploymentService = (MuleDeploymentService) deploymentService;
  }

  public void start() {
    this.deploymentService.initializeVoltron();
    try {
      String runtimeConfigurationServiceUrl = System.getenv("IO_RCS_URL");
      String ioAPIHost = getEnvWithDefault("IO_API_HOST", "localhost");
      int ioAPIPort = Integer.valueOf(getEnvWithDefault("IO_API_PORT", "10101"));

      ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();
      HttpServer httpServer = httpServicexSupplier.get().getServerFactory().create(new HttpServerConfiguration.Builder()
          .setName("integration-orchestrator-api")
          .setPort(ioAPIPort)
          .setHost(ioAPIHost)
          .setReadTimeout(10000)
          .setUsePersistentConnections(true)
          .build());

      HttpClientConfiguration httpClientConfiguration =
          new HttpClientConfiguration.Builder().setName("integration-orchestrator-client").build();

      HttpClient httpClient = httpServicexSupplier.get().getClientFactory().create(httpClientConfiguration);

      httpServer.addRequestHandler(Collections.singleton("POST"), "/", new RequestHandler() {

        @Override
        public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
          String hostValue = requestContext.getRequest().getHeaderValue("Host");
          // TODO make configuration parameterizable
          // TODO for now we will use the host value until we have proper way to map the API host to the actual flow/integration
          try {

            String integrationName = hostValue;
            Application application = IntegrationOrchestratorAPI.this.deploymentService.findApplication(hostValue);
            if (application == null) {
              String runtimeConfigurationUrlForFetchingIntegrationConfig =
                  String.format("%s/integration/%s", runtimeConfigurationServiceUrl, hostValue);
              HttpResponse httpResponse = httpClient.send(HttpRequest.builder()
                  .uri(runtimeConfigurationUrlForFetchingIntegrationConfig)
                  .build());
              ByteArrayInputStream flowsConfiguration = new ByteArrayInputStream(httpResponse.getEntity().getBytes());
              ArtifactAst artifactAst = defaultArtifactAstDeserializer.deserialize(flowsConfiguration, extensionModelResolver);

              IntegrationOrchestratorAPI.this.deploymentService.deploy(artifactAst, integrationName);
              application = IntegrationOrchestratorAPI.this.deploymentService.findApplication(integrationName);
            }
            // TODO harding name of the flow for now until we can discover the exact flow to execute
            Optional<Flow> flow = application.getRegistry().lookupByName("flow");

            // TODO use proper encoding from MuleContext.getEncoding(..)
            // TODO properly configure base path and listener path.
            // TODO we need to refactor de mule-http-connector so we can use the same logic for creating the message
            Result<InputStream, HttpRequestAttributes> requestData =
                HttpRequestToResult.transform(requestContext, Charset.defaultCharset(), new ListenerPath(null, "/"));
            DataTypeParamsBuilder payloadDataType =
                DataType.builder().mediaType(requestData.getAttributes().get().getHeaders().get("content-type"));
            TypedValue<InputStream> payloadTypedValue =
                new TypedValue<>(requestData.getOutput(), payloadDataType.build(), requestData.getLength());
            Message message =
                Message.builder().payload(payloadTypedValue).attributes(TypedValue.of(requestData.getAttributes().get())).build();
            CompletableFuture<ExecutionResult> flowResultFuture = flow.get().execute(InputEvent.create()
                .message(message));

            ExecutionResult executionResult = flowResultFuture.get();
            Event executionResultEvent = executionResult.getEvent();
            TypedValue<Object> responsePayloadTypedValue = executionResultEvent.getMessage().getPayload();

            HttpResponseBuilder httpResponseBuilder = HttpResponse.builder()
                .statusCode(200);
            if (responsePayloadTypedValue != null) {
              if (responsePayloadTypedValue.getDataType().isStreamType()) {
                httpResponseBuilder.entity(new InputStreamHttpEntity((InputStream) responsePayloadTypedValue.getValue()));
              } else if (responsePayloadTypedValue.getDataType().getType().isAssignableFrom(Byte[].class)) {
                httpResponseBuilder.entity(new ByteArrayHttpEntity((byte[]) responsePayloadTypedValue.getValue()));
              } else {
                throw new RuntimeException("unknown response type " + responsePayloadTypedValue.getDataType().getType());
              }
            }
            HttpResponse httpResponse = httpResponseBuilder.build();
            responseCallback.responseReady(httpResponse, new ResponseStatusCallback() {

              @Override
              public void responseSendFailure(Throwable throwable) {
                logger.warn("responseSendFailure invoking flow through HTTP", throwable);
              }

              @Override
              public void responseSendSuccessfully() {
                logger.info("responseSendSuccessfully invoking flow through HTTP");
              }
            });
          } catch (Exception e) {
            logger.error("Error processing http request to trigger flow execution", e);
          }

        }
      });

      httpClient.start();
      httpServer.start();
    } catch (ServerCreationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getEnvWithDefault(String key, String defValue) {
    String value = System.getenv("IO_API_PORT");
    return value != null ? value : defValue;
  }

  // TODO implement graceful shutdown
  public void stop() {

  }
}
