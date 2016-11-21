/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.client;

import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.extension.http.api.request.proxy.ProxyConfig;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.runtime.api.tls.TlsContextFactory;

/**
 * Configuration component that specifies how an {@link HttpClient} should be created.
 *
 * @since 4.0
 */
public class HttpClientConfiguration {

  private final UriParameters uriParameters;
  private final HttpAuthentication authentication;
  private final TlsContextFactory tlsContextFactory;
  private final ProxyConfig proxyConfig;
  private final TcpClientSocketProperties clientSocketProperties;
  private final int maxConnections;
  private final boolean usePersistentConnections;
  private final int connectionIdleTimeout;
  private final String threadNamePrefix;
  private final String ownerName;

  private HttpClientConfiguration(UriParameters uriParameters, HttpAuthentication authentication,
                                  TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig,
                                  TcpClientSocketProperties clientSocketProperties, int maxConnections,
                                  boolean usePersistentConnections, int connectionIdleTimeout, String threadNamePrefix,
                                  String ownerName) {
    this.uriParameters = uriParameters;
    this.authentication = authentication;
    this.tlsContextFactory = tlsContextFactory;
    this.proxyConfig = proxyConfig;
    this.clientSocketProperties = clientSocketProperties;
    this.maxConnections = maxConnections;
    this.usePersistentConnections = usePersistentConnections;
    this.connectionIdleTimeout = connectionIdleTimeout;
    this.threadNamePrefix = threadNamePrefix;
    this.ownerName = ownerName;
  }

  public UriParameters getUriParameters() {
    return uriParameters;
  }

  public HttpAuthentication getAuthentication() {
    return authentication;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

  public TcpClientSocketProperties getClientSocketProperties() {
    return clientSocketProperties;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public boolean isUsePersistentConnections() {
    return usePersistentConnections;
  }

  public int getConnectionIdleTimeout() {
    return connectionIdleTimeout;
  }

  public String getThreadNamePrefix() {
    return threadNamePrefix;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public static class Builder {

    private UriParameters uriParameters;
    private HttpAuthentication authentication;
    private TlsContextFactory tlsContextFactory;
    private ProxyConfig proxyConfig;
    private TcpClientSocketProperties clientSocketProperties;
    private int maxConnections;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private String threadNamePrefix;
    private String ownerName;

    public Builder setUriParameters(UriParameters uriParameters) {
      this.uriParameters = uriParameters;
      return this;
    }

    public Builder setAuthentication(HttpAuthentication authentication) {
      this.authentication = authentication;
      return this;
    }

    public Builder setTlsContextFactory(TlsContextFactory tlsContextFactory) {
      this.tlsContextFactory = tlsContextFactory;
      return this;
    }

    public Builder setProxyConfig(ProxyConfig proxyConfig) {
      this.proxyConfig = proxyConfig;
      return this;
    }

    public Builder setClientSocketProperties(TcpClientSocketProperties clientSocketProperties) {
      this.clientSocketProperties = clientSocketProperties;
      return this;
    }

    public Builder setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    public Builder setUsePersistentConnections(boolean usePersistentConnections) {
      this.usePersistentConnections = usePersistentConnections;
      return this;
    }

    public Builder setConnectionIdleTimeout(int connectionIdleTimeout) {
      this.connectionIdleTimeout = connectionIdleTimeout;
      return this;
    }

    public Builder setThreadNamePrefix(String threadNamePrefix) {
      this.threadNamePrefix = threadNamePrefix;
      return this;
    }

    public Builder setOwnerName(String ownerName) {
      this.ownerName = ownerName;
      return this;
    }

    public HttpClientConfiguration build() {
      return new HttpClientConfiguration(uriParameters, authentication, tlsContextFactory, proxyConfig, clientSocketProperties,
                                         maxConnections, usePersistentConnections, connectionIdleTimeout, threadNamePrefix,
                                         ownerName);
    }
  }
}
