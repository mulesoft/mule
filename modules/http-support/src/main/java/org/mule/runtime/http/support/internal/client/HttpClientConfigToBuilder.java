/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.tcp.TcpClientSocketProperties;
import org.mule.sdk.api.http.client.HttpClientConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;
import org.mule.sdk.api.http.tcp.TcpSocketPropertiesConfigurer;

import java.util.function.Consumer;

public record HttpClientConfigToBuilder(HttpClientConfiguration.Builder builder) implements HttpClientConfig {

  @Override
  public HttpClientConfig setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    builder.setTlsContextFactory(tlsContextFactory);
    return this;
  }

  @Override
  public HttpClientConfig setMaxConnections(int maxConnections) {
    builder.setMaxConnections(maxConnections);
    return this;
  }

  @Override
  public HttpClientConfig setUsePersistentConnections(boolean usePersistentConnections) {
    builder.setUsePersistentConnections(usePersistentConnections);
    return this;
  }

  @Override
  public HttpClientConfig setConnectionIdleTimeout(int connectionIdleTimeout) {
    builder.setConnectionIdleTimeout(connectionIdleTimeout);
    return this;
  }

  @Override
  public HttpClientConfig setStreaming(boolean streaming) {
    builder.setStreaming(streaming);
    return this;
  }

  @Override
  public HttpClientConfig setResponseBufferSize(int responseBufferSize) {
    builder.setResponseBufferSize(responseBufferSize);
    return this;
  }

  @Override
  public HttpClientConfig setName(String name) {
    builder.setName(name);
    return this;
  }

  @Override
  public HttpClientConfig setDecompress(Boolean decompress) {
    builder.setDecompress(decompress);
    return this;
  }

  @Override
  public HttpClientConfig configClientSocketProperties(Consumer<TcpSocketPropertiesConfigurer> configCallback) {
    var propsBuilder = TcpClientSocketProperties.builder();
    var configurer = new TcpSocketPropertiesConfigurerToBuilder(propsBuilder);
    configCallback.accept(configurer);
    builder.setClientSocketProperties(propsBuilder.build());
    return this;
  }

  @Override
  public HttpClientConfig configProxy(Consumer<ProxyConfig> configCallback) {
    var configurer = new ProxyConfigImpl();
    configCallback.accept(configurer);
    builder.setProxyConfig(configurer.build());
    return this;
  }
}
