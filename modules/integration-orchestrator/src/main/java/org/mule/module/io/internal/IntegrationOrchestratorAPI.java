/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.io.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.io.IOUtils;
import org.mule.module.io.internal.dto.ConfigurationDTO;
import org.mule.module.io.internal.dto.IntegrationConfigDTO;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.http.api.HttpHeaders;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
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
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.mule.sdk.api.annotation.param.MediaType.APPLICATION_JSON;

public class IntegrationOrchestratorAPI {

  private static Logger logger = LoggerFactory.getLogger(IntegrationOrchestratorAPI.class);

  private final Supplier<HttpService> httpServicexSupplier;
  private final MuleDeploymentService deploymentService;
  private SchedulerService schedulerService;

  public IntegrationOrchestratorAPI(Supplier<HttpService> httpServiceSupplier, DeploymentService deploymentService) {
    this.httpServicexSupplier = httpServiceSupplier;
    this.deploymentService = (MuleDeploymentService) deploymentService;
  }

  public void start() {
    this.deploymentService.initializeVoltron();
    try {
      String runtimeConfigurationServiceUrl = System.getenv("SERVICE_RCS_URL");
      String icaasServiceUrl = System.getenv("SERVICE_ICAAS_URL");
      String ioAPIHost = getEnvWithDefault("IO_API_HOST", "localhost");
      int ioAPIPort = Integer.valueOf(getEnvWithDefault("IO_API_PORT", "10101"));

      logger.warn("SERVICE_RCS_URL: " + runtimeConfigurationServiceUrl);
      logger.warn("SERVICE_ICAAS_URL: " + icaasServiceUrl);
      logger.warn("IO_API_HOST: " + ioAPIHost);
      logger.warn("IO_API_PORT: " + ioAPIPort);
      URL icaaServiceUrl = new URL(icaasServiceUrl);
      String icaasProtocol = icaaServiceUrl.getProtocol();
      String icaasHost = icaaServiceUrl.getHost();
      String icaasPort = String.valueOf(icaaServiceUrl.getPort());
      logger.warn("ICAAS PROTOCOL: " + icaasProtocol);
      logger.warn("ICAAS HOST: " + icaasHost);
      logger.warn("ICAAS PORT: " + icaasPort);

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

      addIoRequestHandler(runtimeConfigurationServiceUrl, icaasProtocol, icaasHost, icaasPort, defaultArtifactAstDeserializer,
                          httpServer, httpClient);

      logger.warn("Starting HTTP client of IO");
      httpClient.start();

      logger.warn("Starting HTTP server of IO");
      httpServer.start();
    } catch (ServerCreationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addIoRequestHandler(String runtimeConfigurationServiceUrl, String icaasProtocol, String icaasHost, String icaasPort,
                                   ArtifactAstDeserializer defaultArtifactAstDeserializer, HttpServer httpServer,
                                   HttpClient httpClient) {
    httpServer.addRequestHandler(Collections.singleton("POST"), "/", new RequestHandler() {

      @Override
      public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
        String hostValue = requestContext.getRequest().getHeaderValue("Host");
        String integrationId = requestContext.getRequest().getQueryParams().get("integration");

        // TODO make configuration parameterizable
        // TODO for now we will use the host value until we have proper way to map the API host to the actual flow/integration
        try {

          Application application = IntegrationOrchestratorAPI.this.deploymentService.findApplication(integrationId);
          if (application == null) {
            String runtimeConfigurationUrlForFetchingIntegrationConfig =
                String.format("%s/config/integration/%s", runtimeConfigurationServiceUrl, integrationId);

            long initialTime = System.currentTimeMillis();
            HttpResponse httpResponse = httpClient.send(HttpRequest.builder()
                .uri(runtimeConfigurationUrlForFetchingIntegrationConfig)
                .addHeader(HttpHeaders.Names.CONTENT_TYPE, APPLICATION_JSON)
                .build());
            logger.error("Time to fetch integration config in millis: " + (System.currentTimeMillis() - initialTime));

            if (httpResponse.getStatusCode() != 200) {
              String message = "failed getting configuration. Status code is: " + httpResponse.getStatusCode();
              if (httpResponse.getEntity() != null) {
                message = message + ". Response body: " + IOUtils.toString(httpResponse.getEntity().getBytes());
              }
              throw new RuntimeException(message);
            }

            initialTime = System.currentTimeMillis();
            ByteArrayInputStream integrationConfigResponse = new ByteArrayInputStream(httpResponse.getEntity().getBytes());
            Gson gson = new Gson();

            IntegrationConfigDTO integrationConfigDTO =
                gson.fromJson(IOUtils.toString(integrationConfigResponse), IntegrationConfigDTO.class);

            byte[] configBytes = Base64.getDecoder().decode(integrationConfigDTO.getConfig());
            Properties configurationProperties = new Properties();
            // This values are placeholder in the integration configurations
            configurationProperties.put("SERVICE_ICAAS_HOST", icaasHost);
            configurationProperties.put("SERVICE_ICAAS_PORT", icaasPort);
            configurationProperties.put("SERVICE_ICAAS_PROTOCOL", icaasProtocol);
            for (ConfigurationDTO configuration : integrationConfigDTO.getConfigurations()) {
              configurationProperties.put(configuration.getIntegrationConfigName(),
                                          configuration.getIcaasConfigId().toString());
            }

            // TODO we need to extract the
            Domain domain = deploymentService.findDomain("io-domain");
            ExtensionManager extensionModelResolver =
                domain.getRegistry().lookupByType(ExtensionManager.class).get();
            ArtifactAst artifactAst =
                defaultArtifactAstDeserializer
                    .deserialize(new ByteArrayInputStream(configBytes),
                                 extensionName -> extensionModelResolver.getExtension(extensionName).orElse(null));


            IntegrationOrchestratorAPI.this.deploymentService.deploy(artifactAst, integrationId,
                                                                     Optional.of(configurationProperties));
            application = IntegrationOrchestratorAPI.this.deploymentService.findApplication(integrationId);
            logger.error("Time to load integration in millis: " + (System.currentTimeMillis() - initialTime));
          }
          // TODO harding name of the flow for now until we can discover the exact flow to execute
          Optional<Flow> flow = application.getRegistry().lookupByName("flow");

          // TODO use proper encoding from MuleContext.getEncoding(..)
          // TODO properly configure base path and listener path.
          // TODO we need to refactor de mule-http-connector so we can use the same logic for creating the message
          String requestContentType = requestContext.getRequest().getHeaderValue("content-type");
          InputStream requestContent = requestContext.getRequest().getEntity().getContent();
          DataTypeParamsBuilder payloadDataType =
              DataType.builder().mediaType(requestContentType);
          String contentLength = requestContext.getRequest().getHeaderValue("content-length");
          OptionalLong requestLength =
              contentLength != null ? OptionalLong.of(Long.valueOf(contentLength)) : OptionalLong.empty();
          TypedValue<InputStream> payloadTypedValue =
              new TypedValue<>(requestContent, payloadDataType.build(), requestLength);
          Message message =
              Message.builder().payload(payloadTypedValue).build();
          // Message message =
          // Message.builder().payload(payloadTypedValue).attributes(TypedValue.of(requestData.getAttributes().get())).build();
          CompletableFuture<ExecutionResult> flowResultFuture = flow.get().execute(InputEvent.create()
              .message(message));

          ExecutionResult executionResult = flowResultFuture.get();
          Event executionResultEvent = executionResult.getEvent();
          TypedValue<Object> responsePayloadTypedValue = executionResultEvent.getMessage().getPayload();

          HttpResponseBuilder httpResponseBuilder = HttpResponse.builder()
              .statusCode(200);
          if (executionResultEvent.getMessage().getPayload() == null) {
            httpResponseBuilder.entity(new EmptyHttpEntity());
          } else {
            TransformationService transformationService =
                application.getRegistry().lookupByType(TransformationService.class).get();
            Object jsonValue = transformationService.transform(executionResultEvent.getMessage(), DataType.JSON_STRING)
                .getPayload().getValue();
            httpResponseBuilder.entity(new ByteArrayHttpEntity(jsonValue.toString().getBytes()));
            httpResponseBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, APPLICATION_JSON);
          }

          HttpResponse httpResponse = httpResponseBuilder.build();
          responseCallback.responseReady(httpResponse, new ResponseStatusCallback() {

            @Override
            public void responseSendFailure(Throwable throwable) {
              logger.warn("responseSendFailure - invoking flow through HTTP", throwable);
            }

            @Override
            public void responseSendSuccessfully() {
              logger.info("responseSendSuccessfully - invoking flow through HTTP");
            }
          });
        } catch (Exception e) {
          // TODO remove
          e.printStackTrace();
          responseCallback.responseReady(HttpResponse.builder().statusCode(500).build(), new ResponseStatusCallback() {

            @Override
            public void responseSendFailure(Throwable throwable) {
              logger.warn("responseSendFailure but after error - invoking flow through HTTP", throwable);
            }

            @Override
            public void responseSendSuccessfully() {
              logger.info("responseSendSuccessfully but after error - invoking flow through HTTP");
            }
          });
          logger.error("Error processing http request to trigger flow execution", e);
        }

      }
    });
  }

  private static String getEnvWithDefault(String key, String defValue) {
    String value = System.getenv(key);
    return value != null ? value : defValue;
  }

  // TODO implement graceful shutdown
  public void stop() {

  }
}
