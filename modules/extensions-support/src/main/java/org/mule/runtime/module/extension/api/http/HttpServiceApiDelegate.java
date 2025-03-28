/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.client.HttpClientConfigurationBuilder;
import org.mule.sdk.api.http.client.HttpRequestOptions;
import org.mule.sdk.api.http.client.HttpRequestOptionsBuilder;
import org.mule.sdk.api.http.client.auth.HttpAuthenticationBuilder;
import org.mule.sdk.api.http.client.proxy.NtlmProxyConfigBuilder;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfigBuilder;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.response.HttpResponseBuilder;
import org.mule.sdk.api.http.server.HttpServer;
import org.mule.sdk.api.http.server.HttpServerConfigurationBuilder;
import org.mule.sdk.api.http.server.PathAndMethodRequestMatcherBuilder;
import org.mule.sdk.api.http.server.RequestHandler;
import org.mule.sdk.api.http.server.RequestMatcher;
import org.mule.sdk.api.http.tcp.TcpSocketPropertiesBuilder;
import org.mule.sdk.api.http.utils.RequestMatcherRegistryBuilder;

import java.util.function.Consumer;

public class HttpServiceApiDelegate implements org.mule.sdk.api.http.HttpService {

  @Override
  public HttpClient client(Consumer<HttpClientConfigurationBuilder> configBuilder) {
    return null;
  }

  @Override
  public HttpServer server(Consumer<HttpServerConfigurationBuilder> configBuilder) {
    return null;
  }

  @Override
  public RequestMatcherRegistryBuilder<RequestHandler> requestMatcherRegistryBuilder() {
    return null;
  }

  @Override
  public RequestMatcher acceptAllRequests() {
    return null;
  }

  @Override
  public PathAndMethodRequestMatcherBuilder requestMatcherBuilder() {
    return null;
  }

  @Override
  public TcpSocketPropertiesBuilder tcpSocketPropertiesBuilder() {
    return null;
  }

  @Override
  public HttpResponseBuilder responseBuilder(HttpResponse original) {
    return null;
  }

  @Override
  public HttpRequestBuilder requestBuilder() {
    return null;
  }

  @Override
  public HttpRequestOptionsBuilder requestOptionsBuilder() {
    return null;
  }

  @Override
  public HttpRequestOptionsBuilder requestOptionsBuilder(HttpRequestOptions original) {
    return null;
  }

  @Override
  public HttpAuthenticationBuilder authBuilder() {
    return null;
  }

  @Override
  public HttpAuthenticationBuilder basicAuthBuilder(String username, String password) {
    return null;
  }

  @Override
  public HttpAuthenticationBuilder digestAuthBuilder(String username, String password) {
    return null;
  }

  @Override
  public HttpAuthenticationBuilder.HttpNtlmAuthenticationBuilder ntlmAuthBuilder(String username, String password) {
    return null;
  }

  @Override
  public ProxyConfigBuilder<ProxyConfig, ?> proxyConfigBuilder() {
    return null;
  }

  @Override
  public NtlmProxyConfigBuilder ntlmProxyConfigBuilder() {
    return null;
  }
}
