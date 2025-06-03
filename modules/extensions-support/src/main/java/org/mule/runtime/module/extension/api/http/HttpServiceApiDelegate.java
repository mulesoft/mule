/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.runtime.module.extension.api.http.client.HttpClientConfigToBuilder;
import org.mule.runtime.module.extension.api.http.client.HttpClientWrapper;
import org.mule.runtime.module.extension.api.http.message.HttpEntityFactoryImpl;
import org.mule.runtime.module.extension.api.http.message.HttpRequestBuilderWrapper;
import org.mule.runtime.module.extension.api.http.message.HttpResponseBuilderWrapper;
import org.mule.runtime.module.extension.api.http.server.HttpServerConfigToBuilder;
import org.mule.runtime.module.extension.api.http.server.HttpServerWrapper;
import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.client.HttpClientConfig;
import org.mule.sdk.api.http.domain.entity.HttpEntityFactory;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.response.HttpResponseBuilder;
import org.mule.sdk.api.http.server.HttpServer;
import org.mule.sdk.api.http.server.HttpServerConfig;

import java.util.function.Consumer;

import javax.inject.Inject;

public class HttpServiceApiDelegate implements org.mule.sdk.api.http.HttpService {

  @Inject
  private HttpService httpService;

  private final HttpEntityFactory httpEntityFactory = new HttpEntityFactoryImpl();

  @Override
  public HttpClient client(Consumer<HttpClientConfig> configCallback) {
    var builder = new HttpClientConfiguration.Builder();
    var configurer = new HttpClientConfigToBuilder(builder);
    configCallback.accept(configurer);
    HttpClientConfiguration configuration = builder.build();
    try {
      return new HttpClientWrapper(httpService.getClientFactory().create(configuration));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public HttpServer server(Consumer<HttpServerConfig> configCallback)
      throws org.mule.sdk.api.http.server.ServerCreationException {
    var builder = new HttpServerConfiguration.Builder();
    var configurer = new HttpServerConfigToBuilder(builder);
    configCallback.accept(configurer);
    HttpServerConfiguration configuration = builder.build();
    try {
      return new HttpServerWrapper(httpService.getServerFactory().create(configuration));
    } catch (ServerCreationException e) {
      throw new org.mule.sdk.api.http.server.ServerCreationException(e.getMessage());
    }
  }

  @Override
  public HttpResponseBuilder responseBuilder() {
    return new HttpResponseBuilderWrapper(org.mule.runtime.http.api.domain.message.response.HttpResponse.builder());
  }

  @Override
  public HttpResponseBuilder responseBuilder(HttpResponse original) {
    return responseBuilder().statusCode(original.getStatusCode()).reasonPhrase(original.getReasonPhrase());
  }

  @Override
  public HttpRequestBuilder requestBuilder() {
    return new HttpRequestBuilderWrapper(org.mule.runtime.http.api.domain.message.request.HttpRequest.builder());
  }

  @Override
  public HttpRequestBuilder requestBuilder(boolean preserveHeaderCase) {
    return new HttpRequestBuilderWrapper(org.mule.runtime.http.api.domain.message.request.HttpRequest
        .builder(preserveHeaderCase));
  }

  @Override
  public HttpEntityFactory entityFactory() {
    return httpEntityFactory;
  }
}
