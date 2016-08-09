/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener.server;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.module.http.internal.listener.Server;

/**
 * Configuration component that specifies how a {@link Server} should be created.
 *
 * @since 4.0
 */
public class HttpServerConfiguration {

  private final String host;
  private final int port;
  private final TlsContextFactory tlsContextFactory;
  private final boolean usePersistentConnections;
  private final int connectionIdleTimeout;
  private final WorkManagerSource workManagerSource;

  public HttpServerConfiguration(String host, int port, TlsContextFactory tlsContextFactory, boolean usePersistentConnections,
                                 int connectionIdleTimeout, WorkManagerSource workManagerSource) {
    this.host = host;
    this.port = port;
    this.tlsContextFactory = tlsContextFactory;
    this.usePersistentConnections = usePersistentConnections;
    this.connectionIdleTimeout = connectionIdleTimeout;
    this.workManagerSource = workManagerSource;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }

  public boolean isUsePersistentConnections() {
    return usePersistentConnections;
  }

  public int getConnectionIdleTimeout() {
    return connectionIdleTimeout;
  }

  public WorkManagerSource getWorkManagerSource() {
    return workManagerSource;
  }

  public static class Builder {

    private String host;
    private int port;
    private TlsContextFactory tlsContextFactory;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private WorkManagerSource workManagerSource;


    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setTlsContextFactory(TlsContextFactory tlsContextFactory) {
      this.tlsContextFactory = tlsContextFactory;
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

    public Builder setWorkManagerSource(WorkManagerSource workManagerSource) {
      this.workManagerSource = workManagerSource;
      return this;
    }

    public HttpServerConfiguration build() {
      return new HttpServerConfiguration(host, port, tlsContextFactory, usePersistentConnections, connectionIdleTimeout,
                                         workManagerSource);
    }
  }
}
