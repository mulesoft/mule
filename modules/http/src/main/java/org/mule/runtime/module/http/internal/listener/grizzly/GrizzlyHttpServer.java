/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.grizzly;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.module.http.internal.listener.HttpListenerRegistry;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.ServerAddress;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

/**
 * Grizzly based implementation of an {@link HttpServer}.
 */
public class GrizzlyHttpServer implements HttpServer, MuleContextAware, Supplier<ExecutorService> {

  private final TCPNIOTransport transport;
  private final ServerAddress serverAddress;
  private final HttpListenerRegistry listenerRegistry;
  private TCPNIOServerConnection serverConnection;
  private Scheduler scheduler;
  private boolean stopped = true;
  private boolean stopping;
  private MuleContext muleContext;
  private String ownerName;

  public GrizzlyHttpServer(ServerAddress serverAddress, TCPNIOTransport transport, HttpListenerRegistry listenerRegistry,
                           Supplier<Scheduler> schedulerSource) {
    this.serverAddress = serverAddress;
    this.transport = transport;
    this.listenerRegistry = listenerRegistry;
    this.scheduler = schedulerSource.get();
  }

  @Override
  public synchronized void start() throws IOException {
    serverConnection = transport.bind(serverAddress.getIp(), serverAddress.getPort());
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
    try {
      //TODO - MULE-11115: Get rid of muleContext once we have a stop() method
      scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
    } finally {
      scheduler = null;
    }
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
  public RequestHandlerManager addRequestHandler(PathAndMethodRequestMatcher requestMatcher, RequestHandler requestHandler) {
    return this.listenerRegistry.addRequestHandler(this, requestHandler, requestMatcher);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public ExecutorService get() {
    return scheduler;
  }
}
