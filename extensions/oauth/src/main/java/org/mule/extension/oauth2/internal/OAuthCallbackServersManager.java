/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerConfiguration;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestHandler;
import org.mule.service.http.api.server.RequestHandlerManager;
import org.mule.service.http.api.server.ServerAddress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class OAuthCallbackServersManager {

  @Inject
  private HttpService httpService;

  private Map<Integer, CountedHttpServer> serversByPort = new HashMap<>();

  public synchronized HttpServer getServer(HttpServerConfiguration serverConfiguration) throws ConnectionException, IOException {
    if (!serversByPort.containsKey(serverConfiguration.getPort())) {
      serversByPort.put(serverConfiguration.getPort(),
                        new CountedHttpServer(httpService.getServerFactory().create(serverConfiguration)));
    }

    return serversByPort.get(serverConfiguration.getPort());
  }

  private class CountedHttpServer implements HttpServer {

    private AtomicInteger count = new AtomicInteger(0);
    private final HttpServer server;
    private ConcurrentMap<PathAndMethodRequestMatcher, RequestHandlerManager> registeredHandlerMatchers =
        new ConcurrentHashMap<>();

    CountedHttpServer(HttpServer server) {
      this.server = server;
    }

    @Override
    public void start() throws IOException {
      if (count.getAndIncrement() == 0) {
        server.start();
      }
    }

    @Override
    public void stop() {
      if (count.decrementAndGet() == 0) {
        server.stop();
      }
    }

    @Override
    public void dispose() {
      if (count.get() == 0) {
        server.dispose();
        serversByPort.remove(getServerAddress().getPort());
      }
    }

    @Override
    public ServerAddress getServerAddress() {
      return server.getServerAddress();
    }

    @Override
    public boolean isStopping() {
      return server.isStopping();
    }

    @Override
    public boolean isStopped() {
      return server.isStopped();
    }

    @Override
    public RequestHandlerManager addRequestHandler(PathAndMethodRequestMatcher pathAndMethodRequestMatcher,
                                                   RequestHandler requestHandler) {
      synchronized (registeredHandlerMatchers) {
        if (!registeredHandlerMatchers.containsKey(pathAndMethodRequestMatcher)) {
          registeredHandlerMatchers
              .put(pathAndMethodRequestMatcher,
                   new CountedRequestHandlerManager(pathAndMethodRequestMatcher,
                                                    server.addRequestHandler(pathAndMethodRequestMatcher, requestHandler)));
        }
        return registeredHandlerMatchers.get(pathAndMethodRequestMatcher);
      }
    }

    private class CountedRequestHandlerManager implements RequestHandlerManager {

      private AtomicInteger count = new AtomicInteger(0);
      private PathAndMethodRequestMatcher requestMatcher;
      private RequestHandlerManager requestHandler;

      public CountedRequestHandlerManager(PathAndMethodRequestMatcher requestMatcher, RequestHandlerManager requestHandler) {
        this.requestMatcher = requestMatcher;
        this.requestHandler = requestHandler;
      }

      @Override
      public void start() {
        if (count.getAndIncrement() == 0) {
          requestHandler.start();
        }
      }

      @Override
      public void stop() {
        if (count.decrementAndGet() == 0) {
          requestHandler.stop();
        }
      }

      @Override
      public void dispose() {
        if (count.get() == 0) {
          requestHandler.dispose();
          registeredHandlerMatchers.remove(requestMatcher);
        }
      }
    }
  }
}
