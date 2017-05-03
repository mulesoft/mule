/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server;


import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static org.mule.services.http.impl.service.server.grizzly.IdleExecutor.IDLE_TIMEOUT_THREADS_PREFIX_NAME;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.service.http.api.server.HttpServerFactory;
import org.mule.service.http.api.server.ServerAddress;
import org.mule.service.http.api.server.ServerNotFoundException;
import org.mule.service.http.api.tcp.TcpServerSocketProperties;
import org.mule.services.http.impl.service.server.grizzly.GrizzlyServerManager;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Grizzly based {@link HttpServerFactory}.
 *
 * @since 4.0
 */
public class HttpListenerConnectionManager implements ContextHttpServerFactory, Initialisable, Disposable {

  public static final String SERVER_ALREADY_EXISTS_FORMAT =
      "A server in port(%s) already exists for ip(%s) or one overlapping it (0.0.0.0).";
  private static final String LISTENER_THREAD_NAME_PREFIX = "http.listener";

  private final SchedulerService schedulerService;
  private final SchedulerConfig schedulersConfig;
  private Scheduler selectorScheduler;
  private Scheduler workerScheduler;
  private Scheduler idleTimeoutScheduler;
  private final HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
  private HttpServerManager httpServerManager;

  private AtomicBoolean initialized = new AtomicBoolean(false);

  public HttpListenerConnectionManager(SchedulerService schedulerService, SchedulerConfig schedulersConfig) {
    this.schedulerService = schedulerService;
    this.schedulersConfig = schedulersConfig;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (initialized.getAndSet(true)) {
      return;
    }

    // TODO - MULE-11116: Analyze how to allow users to configure this
    TcpServerSocketProperties tcpServerSocketProperties = new DefaultTcpServerSocketProperties();

    selectorScheduler = schedulerService.customScheduler(schedulersConfig
        .withMaxConcurrentTasks(getRuntime().availableProcessors() + 1).withName(LISTENER_THREAD_NAME_PREFIX), MAX_VALUE);
    workerScheduler = schedulerService.ioScheduler(schedulersConfig);
    idleTimeoutScheduler =
        schedulerService.ioScheduler(schedulersConfig.withName(LISTENER_THREAD_NAME_PREFIX + IDLE_TIMEOUT_THREADS_PREFIX_NAME));
    httpServerManager = new GrizzlyServerManager(selectorScheduler, workerScheduler, idleTimeoutScheduler, httpListenerRegistry,
                                                 tcpServerSocketProperties);

  }

  @Override
  public synchronized void dispose() {
    httpServerManager.dispose();
    idleTimeoutScheduler.stop();
    workerScheduler.stop();
    selectorScheduler.stop();
  }

  @Override
  public HttpServer create(HttpServerConfiguration configuration, String context) throws ConnectionException {
    ServerAddress serverAddress;
    String host = configuration.getHost();
    try {
      serverAddress = createServerAddress(host, configuration.getPort());
    } catch (UnknownHostException e) {
      throw new ConnectionException(String.format("Cannot resolve host %s", host), e);
    }

    TlsContextFactory tlsContextFactory = configuration.getTlsContextFactory();
    HttpServer httpServer;
    if (tlsContextFactory == null) {
      httpServer = createServer(serverAddress, configuration.getSchedulerSupplier(), configuration.isUsePersistentConnections(),
                                configuration.getConnectionIdleTimeout(), new ServerIdentifier(context, configuration.getName()));
    } else {
      httpServer = createSslServer(serverAddress, tlsContextFactory, configuration.getSchedulerSupplier(),
                                   configuration.isUsePersistentConnections(), configuration.getConnectionIdleTimeout(),
                                   new ServerIdentifier(context, configuration.getName()));
    }

    return httpServer;
  }

  @Override
  public HttpServer lookup(ServerIdentifier identifier) throws ServerNotFoundException {
    return httpServerManager.lookupServer(identifier);
  }

  public HttpServer createServer(ServerAddress serverAddress,
                                 Supplier<Scheduler> schedulerSupplier, boolean usePersistentConnections,
                                 int connectionIdleTimeout, ServerIdentifier identifier) {
    if (!containsServerFor(serverAddress, identifier)) {
      try {
        return httpServerManager.createServerFor(serverAddress, schedulerSupplier, usePersistentConnections,
                                                 connectionIdleTimeout, identifier);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
    }
  }

  public boolean containsServerFor(ServerAddress serverAddress, ServerIdentifier identifier) {
    return httpServerManager.containsServerFor(serverAddress, identifier);
  }

  public HttpServer createSslServer(ServerAddress serverAddress, TlsContextFactory tlsContext,
                                    Supplier<Scheduler> schedulerSupplier, boolean usePersistentConnections,
                                    int connectionIdleTimeout, ServerIdentifier identifier) {
    if (!containsServerFor(serverAddress, identifier)) {
      try {
        return httpServerManager.createSslServerFor(tlsContext, schedulerSupplier, serverAddress, usePersistentConnections,
                                                    connectionIdleTimeout, identifier);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format(SERVER_ALREADY_EXISTS_FORMAT, serverAddress.getPort(), serverAddress.getIp())));
    }
  }

  /**
   * Creates the server address object with the IP and port that a server should bind to.
   */
  private ServerAddress createServerAddress(String host, int port) throws UnknownHostException {
    return new DefaultServerAddress(NetworkUtils.getLocalHostIp(host), port);
  }

  private class DefaultTcpServerSocketProperties implements TcpServerSocketProperties {

    @Override
    public Integer getSendBufferSize() {
      return null;
    }

    @Override
    public Integer getReceiveBufferSize() {
      return null;
    }

    @Override
    public Integer getClientTimeout() {
      return null;
    }

    @Override
    public Boolean getSendTcpNoDelay() {
      return true;
    }

    @Override
    public Integer getLinger() {
      return null;
    }

    @Override
    public Boolean getKeepAlive() {
      return false;
    }

    @Override
    public Boolean getReuseAddress() {
      return true;
    }

    @Override
    public Integer getReceiveBacklog() {
      return 50;
    }

    @Override
    public Integer getServerTimeout() {
      return null;
    }
  }

}
