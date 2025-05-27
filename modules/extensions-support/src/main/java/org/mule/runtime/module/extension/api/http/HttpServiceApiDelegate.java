/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.runtime.http.api.tcp.TcpClientSocketProperties;
import org.mule.runtime.http.api.tcp.TcpClientSocketPropertiesBuilder;
import org.mule.runtime.module.extension.api.http.client.HttpClientWrapper;
import org.mule.runtime.module.extension.api.http.server.HttpServerWrapper;
import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.client.HttpClientConfigurer;
import org.mule.sdk.api.http.client.auth.HttpAuthenticationBuilder;
import org.mule.sdk.api.http.client.proxy.NtlmProxyConfigBuilder;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfigBuilder;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.domain.message.response.HttpResponseBuilder;
import org.mule.sdk.api.http.server.HttpServer;
import org.mule.sdk.api.http.server.HttpServerConfigurer;
import org.mule.sdk.api.http.server.PathAndMethodRequestMatcherBuilder;
import org.mule.sdk.api.http.server.RequestHandler;
import org.mule.sdk.api.http.server.RequestMatcher;
import org.mule.sdk.api.http.tcp.TcpSocketPropertiesConfigurer;
import org.mule.sdk.api.http.utils.RequestMatcherRegistryBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Inject;

public class HttpServiceApiDelegate implements org.mule.sdk.api.http.HttpService {

  @Inject
  private HttpService httpService;

  @Override
  public HttpClient client(Consumer<HttpClientConfigurer> configBuilder) {
    var builder = new HttpClientConfiguration.Builder();
    var configurer = new HttpClientConfigurerToBuilder(builder);
    configBuilder.accept(configurer);
    HttpClientConfiguration configuration = builder.build();
    try {
      return new HttpClientWrapper(httpService.getClientFactory().create(configuration));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public HttpServer server(Consumer<HttpServerConfigurer> configBuilder)
      throws org.mule.sdk.api.http.server.ServerCreationException {
    var builder = new HttpServerConfiguration.Builder();
    var configurer = new HttpServerConfigurerToBuilder(builder);
    configBuilder.accept(configurer);
    HttpServerConfiguration configuration = builder.build();
    try {
      return new HttpServerWrapper(httpService.getServerFactory().create(configuration));
    } catch (ServerCreationException e) {
      throw new org.mule.sdk.api.http.server.ServerCreationException(e.getMessage());
    }
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
  public HttpResponseBuilder responseBuilder(HttpResponse original) {
    return null;
  }

  @Override
  public HttpRequestBuilder requestBuilder() {
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

  private record HttpServerConfigurerToBuilder(HttpServerConfiguration.Builder builder) implements HttpServerConfigurer {

    @Override
    public HttpServerConfigurer setHost(String host) {
      builder.setHost(host);
      return this;
    }

    @Override
    public HttpServerConfigurer setPort(int port) {
      builder.setPort(port);
      return this;
    }

    @Override
    public HttpServerConfigurer setTlsContextFactory(TlsContextFactory tlsContextFactory) {
      builder.setTlsContextFactory(tlsContextFactory);
      return this;
    }

    @Override
    public HttpServerConfigurer setUsePersistentConnections(boolean usePersistentConnections) {
      builder.setUsePersistentConnections(usePersistentConnections);
      return this;
    }

    @Override
    public HttpServerConfigurer setConnectionIdleTimeout(int connectionIdleTimeout) {
      builder.setConnectionIdleTimeout(connectionIdleTimeout);
      return this;
    }

    @Override
    public HttpServerConfigurer setSchedulerSupplier(Supplier<Scheduler> schedulerSupplier) {
      builder.setSchedulerSupplier(schedulerSupplier);
      return this;
    }

    @Override
    public HttpServerConfigurer setName(String name) {
      builder.setName(name);
      return this;
    }

    @Override
    public HttpServerConfigurer setReadTimeout(long readTimeout) {
      builder.setReadTimeout(readTimeout);
      return this;
    }
  }


  private record HttpClientConfigurerToBuilder(HttpClientConfiguration.Builder builder) implements HttpClientConfigurer {

    @Override
    public HttpClientConfigurer setTlsContextFactory(TlsContextFactory tlsContextFactory) {
      builder.setTlsContextFactory(tlsContextFactory);
      return this;
    }

    @Override
    public HttpClientConfigurer setProxyConfig(ProxyConfig proxyConfig) {
      // builder.setProxyConfig(proxyConfig);
      return this;
    }

    @Override
    public HttpClientConfigurer setMaxConnections(int maxConnections) {
      builder.setMaxConnections(maxConnections);
      return this;
    }

    @Override
    public HttpClientConfigurer setUsePersistentConnections(boolean usePersistentConnections) {
      builder.setUsePersistentConnections(usePersistentConnections);
      return this;
    }

    @Override
    public HttpClientConfigurer setConnectionIdleTimeout(int connectionIdleTimeout) {
      builder.setConnectionIdleTimeout(connectionIdleTimeout);
      return this;
    }

    @Override
    public HttpClientConfigurer setStreaming(boolean streaming) {
      builder.setStreaming(streaming);
      return this;
    }

    @Override
    public HttpClientConfigurer setResponseBufferSize(int responseBufferSize) {
      builder.setResponseBufferSize(responseBufferSize);
      return this;
    }

    @Override
    public HttpClientConfigurer setName(String name) {
      builder.setName(name);
      return this;
    }

    @Override
    public HttpClientConfigurer setDecompress(Boolean decompress) {
      builder.setDecompress(decompress);
      return this;
    }

    @Override
    public HttpClientConfigurer configClientSocketProperties(Consumer<TcpSocketPropertiesConfigurer> configurerConsumer) {
      var propsBuilder = TcpClientSocketProperties.builder();
      var configurer = new TcpSocketPropertiesConfigurerToBuilder(propsBuilder);
      configurerConsumer.accept(configurer);
      builder.setClientSocketProperties(propsBuilder.build());
      return this;
    }
  }

  private record TcpSocketPropertiesConfigurerToBuilder(TcpClientSocketPropertiesBuilder builder)
      implements TcpSocketPropertiesConfigurer {

    @Override
    public TcpSocketPropertiesConfigurer sendBufferSize(Integer sendBufferSize) {
      builder.sendBufferSize(sendBufferSize);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer receiveBufferSize(Integer receiveBufferSize) {
      builder.receiveBufferSize(receiveBufferSize);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer clientTimeout(Integer clientTimeout) {
      builder.clientTimeout(clientTimeout);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer sendTcpNoDelay(Boolean sendTcpNoDelay) {
      builder.sendTcpNoDelay(sendTcpNoDelay);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer linger(Integer linger) {
      builder.linger(linger);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer keepAlive(Boolean keepAlive) {
      builder.keepAlive(keepAlive);
      return this;
    }

    @Override
    public TcpSocketPropertiesConfigurer connectionTimeout(Integer connectionTimeout) {
      builder.connectionTimeout(connectionTimeout);
      return this;
    }
  }
}
