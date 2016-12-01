/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;

import java.util.function.Supplier;

/**
 * Configuration component that specifies how a {@link HttpServer} should be created.
 *
 * @since 4.0
 */
public class HttpServerConfiguration {

  private final String host;
  private final int port;
  private final TlsContextFactory tlsContextFactory;
  private final boolean usePersistentConnections;
  private final int connectionIdleTimeout;
  private final Supplier<Scheduler> schedulerSupplier;
  private final String ownerName;

  HttpServerConfiguration(String host, int port, TlsContextFactory tlsContextFactory, boolean usePersistentConnections,
                          int connectionIdleTimeout, String ownerName, Supplier<Scheduler> schedulerSupplier) {
    this.host = host;
    this.port = port;
    this.tlsContextFactory = tlsContextFactory;
    this.usePersistentConnections = usePersistentConnections;
    this.connectionIdleTimeout = connectionIdleTimeout;
    this.schedulerSupplier = schedulerSupplier;
    this.ownerName = ownerName;
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

  public Supplier<Scheduler> getSchedulerSupplier() {
    return schedulerSupplier;
  }

  public static class Builder {

    private String host;
    private int port;
    private TlsContextFactory tlsContextFactory;
    private boolean usePersistentConnections;
    private int connectionIdleTimeout;
    private String ownerName;
    private Supplier<Scheduler> schedulerSupplier;

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

    public Builder setSchedulerSupplier(Supplier<Scheduler> schedulerSupplier) {
      this.schedulerSupplier = schedulerSupplier;
      return this;
    }

    public Builder setOwnerName(String ownerName) {
      this.ownerName = ownerName;
      return this;
    }

    public HttpServerConfiguration build() {
      return new HttpServerConfiguration(host, port, tlsContextFactory, usePersistentConnections, connectionIdleTimeout,
                                         ownerName, schedulerSupplier);
    }
  }
}
