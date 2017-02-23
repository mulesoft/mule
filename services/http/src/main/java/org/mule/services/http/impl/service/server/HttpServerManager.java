/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.ServerAddress;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A Server managers is in charge to handle all ServerSocket connections and to handle incoming requests to an Execution Service
 * for processing.
 */
public interface HttpServerManager {

  /**
   * @param serverAddress address of the server
   * @return true if there's already a server created for that port and either the same host or an overlapping one (0.0.0.0 or any
   *         other if the serverAddress host is 0.0.0.0), false otherwise.
   */
  boolean containsServerFor(ServerAddress serverAddress);

  /**
   *
   * @param serverAddress address of the server
   * @param schedulerSupplier work manager source to use for retrieving an {@link Executor} for processing this server requests
   * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
   * @param connectionIdleTimeout the amount of milliseconds to keep open an idle connection @return the create Server handler
   * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
   */
  HttpServer createServerFor(ServerAddress serverAddress, Supplier<Scheduler> schedulerSupplier,
                             boolean usePersistentConnections,
                             int connectionIdleTimeout)
      throws IOException;

  /**
   *
   * @param tlsContextFactory
   * @param schedulerSupplier work manager source to use for retrieving an {@link Executor} for processing this server requests
   * @param serverAddress address of the server
   * @param usePersistentConnections if true, the connections will be kept open for subsequent requests
   * @param connectionIdleTimeout the amount of milliseconds to keep open an idle connection
   * @return the create Server handler
   * @throws IOException if it was not possible to create the Server. Most likely because the host and port is already in use.
   */
  HttpServer createSslServerFor(TlsContextFactory tlsContextFactory, Supplier<Scheduler> schedulerSupplier,
                                ServerAddress serverAddress,
                                boolean usePersistentConnections, int connectionIdleTimeout)
      throws IOException;

  /**
   *
   * Frees all the resource hold by the server manager. The client is responsible for stopping all the server prior to call
   * dispose.
   */
  void dispose();
}
