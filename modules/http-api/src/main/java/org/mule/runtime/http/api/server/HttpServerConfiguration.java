/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;

import java.util.function.Supplier;

/**
 * Configuration component that specifies how a {@link HttpServer} should be created. Instances can only be obtained through an
 * {@link HttpServerConfiguration.Builder}.
 *
 * @since 4.0
 */
public class HttpServerConfiguration {

  private final String host;
  private final int port;
  private final TlsContextFactory tlsContextFactory;
  private final boolean usePersistentConnections;
  private final int connectionIdleTimeout;
  private final String name;
  private final Supplier<Scheduler> schedulerSupplier;

  HttpServerConfiguration(String host, int port, TlsContextFactory tlsContextFactory, boolean usePersistentConnections,
                          int connectionIdleTimeout, String name, Supplier<Scheduler> schedulerSupplier) {
    this.host = host;
    this.port = port;
    this.tlsContextFactory = tlsContextFactory;
    this.usePersistentConnections = usePersistentConnections;
    this.connectionIdleTimeout = connectionIdleTimeout;
    this.name = name;
    this.schedulerSupplier = schedulerSupplier;
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

  public String getName() {
    return name;
  }

  public Supplier<Scheduler> getSchedulerSupplier() {
    return schedulerSupplier;
  }

  /**
   * Builder for {@link HttpServerConfiguration}s. At the very least, a host, a port and a name must be provided.
   */
  public static class Builder {

    private String host;
    private int port;
    private TlsContextFactory tlsContextFactory;
    private boolean usePersistentConnections = true;
    private int connectionIdleTimeout = 30000;
    private Supplier<Scheduler> schedulerSupplier;
    private String name;

    /**
     * Defines the host where the requests will be sent to the {@link HttpServer}. Must be provided.
     *
     * @param host where to establish the server
     * @return this builder
     */
    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    /**
     * Defines the port where the requests will be received by the {@link HttpServer}. Must be provided.
     *
     * @param port where to listen
     * @return this builder
     */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

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
     * Defines a {@link Supplier} for a {@link Scheduler} that will be used by the {@link HttpServer} to process requests. Must be
     * specified if the server won't be associated to a flow where a processing strategy will define the scheduling.
     *
     * @param schedulerSupplier
     * @return this builder
     */
    public Builder setSchedulerSupplier(Supplier<Scheduler> schedulerSupplier) {
      this.schedulerSupplier = schedulerSupplier;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * @return a {@link HttpServerConfiguration} as specified.
     */
    public HttpServerConfiguration build() {
      checkNotNull(host, "A host is mandatory");
      checkNotNull(port, "Port is mandatory");
      checkNotNull(name, "Name is mandatory");
      return new HttpServerConfiguration(host, port, tlsContextFactory, usePersistentConnections, connectionIdleTimeout,
                                         name, schedulerSupplier);
    }
  }
}
