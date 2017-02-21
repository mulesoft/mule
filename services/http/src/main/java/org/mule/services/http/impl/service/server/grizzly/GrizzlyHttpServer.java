/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.ServerAddress;
import org.mule.services.http.impl.service.server.HttpListenerRegistry;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

/**
 * Grizzly based implementation of an {@link HttpServer}.
 */
public class GrizzlyHttpServer implements HttpServer, Supplier<ExecutorService> {

  private final TCPNIOTransport transport;
  private final ServerAddress serverAddress;
  private final HttpListenerRegistry listenerRegistry;
  private TCPNIOServerConnection serverConnection;
  private Supplier<Scheduler> schedulerSource;
  private Runnable schedulerDisposer;
  private Scheduler scheduler;
  private boolean stopped = true;
  private boolean stopping;
  private String ownerName;

  public GrizzlyHttpServer(ServerAddress serverAddress, TCPNIOTransport transport, HttpListenerRegistry listenerRegistry,
                           Supplier<Scheduler> schedulerSource, Runnable schedulerDisposer) {
    this.serverAddress = serverAddress;
    this.transport = transport;
    this.listenerRegistry = listenerRegistry;
    this.schedulerSource = schedulerSource;
    this.schedulerDisposer = schedulerDisposer;
  }

  @Override
  public synchronized void start() throws IOException {
    this.scheduler = schedulerSource != null ? schedulerSource.get() : null;
    serverConnection = transport.bind(serverAddress.getIp(), serverAddress.getPort());
    serverConnection.addCloseListener((closeable, type) -> {
      try {
        // TODO MULE-11115 Add a stop() method to Scheduler
        scheduler.stop(5000, MILLISECONDS);
      } finally {
        scheduler = null;
      }
      schedulerDisposer.run();
    });
    stopped = false;
  }

  @Override
  public synchronized void stop() {
    stopping = true;
    try {
      transport.unbind(serverConnection);
    } finally {
      stopping = false;
    }
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  @Override
  public ServerAddress getServerAddress() {
    return serverAddress;
  }

  @Override
  public boolean isStopping() {
    return stopping;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  @Override
  public RequestHandlerManager addRequestHandler(Collection<String> methods, String path, RequestHandler requestHandler) {
    return this.listenerRegistry.addRequestHandler(this, requestHandler,
                                                   new ListenerRequestMatcher(new DefaultMethodRequestMatcher(methods
                                                       .toArray(new String[methods.size()])),
                                                                              path));
  }

  @Override
  public RequestHandlerManager addRequestHandler(String path, RequestHandler requestHandler) {
    return this.listenerRegistry.addRequestHandler(this, requestHandler,
                                                   new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path));
  }

  @Override
  public ExecutorService get() {
    return scheduler;
  }
}
