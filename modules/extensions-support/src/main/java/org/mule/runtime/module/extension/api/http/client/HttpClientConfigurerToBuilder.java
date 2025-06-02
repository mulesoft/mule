/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.tcp.TcpClientSocketProperties;
import org.mule.sdk.api.http.client.HttpClientConfigurer;
import org.mule.sdk.api.http.client.auth.HttpAuthenticationConfigurer;
import org.mule.sdk.api.http.client.proxy.ProxyConfigurer;
import org.mule.sdk.api.http.tcp.TcpSocketPropertiesConfigurer;

import java.util.Objects;
import java.util.function.Consumer;

public class HttpClientConfigurerToBuilder implements HttpClientConfigurer {

  private final HttpClientConfiguration.Builder builder;

  public HttpClientConfigurerToBuilder(HttpClientConfiguration.Builder builder) {
    this.builder = builder;
  }

  @Override
  public HttpClientConfigurer setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    builder.setTlsContextFactory(tlsContextFactory);
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

  @Override
  public HttpClientConfigurer configProxy(Consumer<ProxyConfigurer> configurerConsumer) {
    var configurer = new ProxyConfigurerImpl();
    configurerConsumer.accept(configurer);
    builder.setProxyConfig(configurer.build());
    return this;
  }

  public HttpClientConfiguration.Builder builder() {
    return builder;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    var that = (HttpClientConfigurerToBuilder) obj;
    return Objects.equals(this.builder, that.builder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(builder);
  }

  @Override
  public String toString() {
    return "HttpClientConfigurerToBuilder[" +
        "builder=" + builder + ']';
  }

}
