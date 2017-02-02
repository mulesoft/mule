/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.client;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.service.http.api.client.proxy.ProxyConfig;
import org.mule.service.http.api.tcp.TcpClientSocketProperties;

/**
 * Configuration component that specifies how an {@link HttpClient} should be created. Instances can only be obtained through an
 * {@link HttpClientConfiguration.Builder}.
 *
 * @since 4.0
 */
public class HttpClientConfiguration {

  private final TlsContextFactory tlsContextFactory;
  private final ProxyConfig proxyConfig;
  private final TcpClientSocketProperties clientSocketProperties;
  private final int maxConnections;
  private final boolean usePersistentConnections;
  private final int connectionIdleTimeout;
  private final String threadNamePrefix;
  private final String ownerName;

  HttpClientConfiguration(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig,
                          TcpClientSocketProperties clientSocketProperties, int maxConnections,
                          boolean usePersistentConnections, int connectionIdleTimeout, String threadNamePrefix,
                          String ownerName) {
    this.tlsContextFactory = tlsContextFactory;
    this.proxyConfig = proxyConfig;
    this.clientSocketProperties = clientSocketProperties;
    this.maxConnections = maxConnections;
    this.usePersistentConnections = usePersistentConnections;
    this.connectionIdleTimeout = connectionIdleTimeout;
    this.threadNamePrefix = threadNamePrefix;
    this.ownerName = ownerName;
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

  /**
   * Builder of {@link HttpClientConfiguration}s. At the very least, a thread name prefix and an owner name must be provided.
   */
  public static class Builder {

    private TlsContextFactory tlsContextFactory;
    private ProxyConfig proxyConfig;
    private TcpClientSocketProperties clientSocketProperties;
    private int maxConnections = -1;
    private boolean usePersistentConnections = true;
    private int connectionIdleTimeout = 30000;
    private String threadNamePrefix;
    private String ownerName;

    /**
     * Required exclusively for HTTPS, this defines through a {@link TlsContextFactory} all the TLS related data to establish
     * such connections. Set to {@code null} by default.
     *
     * @param tlsContextFactory a {@link TlsContextFactory} with the required data.
     * @return this builder
     */
    public Builder setTlsContextFactory(TlsContextFactory tlsContextFactory) {
      this.tlsContextFactory = tlsContextFactory;
      return this;
    }

    /**
     * Required when connecting through a proxy, this defines the relevant data to do so using a {@link ProxyConfig}. Set to
     * {@code null} by default.
     *
     * @param proxyConfig a {@link ProxyConfig} specifying the proxy data
     * @return this builder
     */
    public Builder setProxyConfig(ProxyConfig proxyConfig) {
      this.proxyConfig = proxyConfig;
      return this;
    }

    /**
     * Optionally defines TCP specific properties like the socket connection timeout, for example, via a
     * {@link TcpClientSocketProperties}. Set to {@code null} by default, transport default values will be used in that case.
     *
     * @param clientSocketProperties a {@link TcpClientSocketProperties} specifying each property
     * @return this builder
     */
    public Builder setClientSocketProperties(TcpClientSocketProperties clientSocketProperties) {
      this.clientSocketProperties = clientSocketProperties;
      return this;
    }

    /**
     * Defines the maximum number of outbound connections that will be kept open at the same time. Unlimited by default.
     *
     * @param maxConnections number of connections to allow
     * @return this builder
     */
    public Builder setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    /**
     * Defines if connections should be kept after a request is completed or closed. Default value is {@code true}.
     *
     * @param usePersistentConnections {@code boolean} specifying the decision
     * @return this builder
     */
    public Builder setUsePersistentConnections(boolean usePersistentConnections) {
      this.usePersistentConnections = usePersistentConnections;
      return this;
    }

    /**
     * Defines the number of milliseconds that a connection can remain idle before being closed. Only relevant if persistent
     * connections are used, the default value is 30 seconds.
     *
     * @param connectionIdleTimeout timeout value (in milliseconds)
     * @return this builder
     */
    public Builder setConnectionIdleTimeout(int connectionIdleTimeout) {
      this.connectionIdleTimeout = connectionIdleTimeout;
      return this;
    }

    /**
     * Defines the prefix to use to name the {@link HttpClient}'s threads. Must be specified.
     *
     * @param threadNamePrefix a {@link String} representing the prefix
     * @return this builder
     */
    public Builder setThreadNamePrefix(String threadNamePrefix) {
      this.threadNamePrefix = threadNamePrefix;
      return this;
    }

    /**
     * Defines the name of the {@link HttpClient}'s owner. Must be specified.
     *
     * @param ownerName a {@link String} with the name
     * @return this builder
     */
    public Builder setOwnerName(String ownerName) {
      this.ownerName = ownerName;
      return this;
    }

    /**
     * @return an {@link HttpClientConfiguration} as specified.
     */
    public HttpClientConfiguration build() {
      return new HttpClientConfiguration(tlsContextFactory, proxyConfig, clientSocketProperties, maxConnections,
                                         usePersistentConnections, connectionIdleTimeout, threadNamePrefix, ownerName);
    }
  }
}
