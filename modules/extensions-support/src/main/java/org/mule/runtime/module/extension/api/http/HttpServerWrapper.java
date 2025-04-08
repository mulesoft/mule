/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.sdk.api.http.HttpConstants;
import org.mule.sdk.api.http.server.HttpServer;
import org.mule.sdk.api.http.server.RequestHandler;
import org.mule.sdk.api.http.server.EndpointAvailabilityHandler;
import org.mule.sdk.api.http.server.ServerAddress;
import org.mule.sdk.api.http.sse.server.SseClient;
import org.mule.sdk.api.http.sse.server.SseEndpointManager;
import org.mule.sdk.api.http.sse.server.SseRequestContext;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

public class HttpServerWrapper implements HttpServer {

  private final org.mule.runtime.http.api.server.HttpServer delegate;
  private final ServerAddress serverAddress;

  public HttpServerWrapper(org.mule.runtime.http.api.server.HttpServer delegate) {
    this.delegate = delegate;
    this.serverAddress = new ServerAddressWrapper(delegate.getServerAddress());
  }

  @Override
  public void start() throws IOException {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public ServerAddress getServerAddress() {
    return serverAddress;
  }

  @Override
  public HttpConstants.Protocol getProtocol() {
    return HttpConstants.Protocol.valueOf(delegate.getProtocol().name());
  }

  @Override
  public boolean isStopping() {
    return delegate.isStopping();
  }

  @Override
  public boolean isStopped() {
    return delegate.isStopped();
  }

  @Override
  public void enableTls(TlsContextFactory tlsContextFactory) {
    delegate.enableTls(tlsContextFactory);
  }

  @Override
  public void disableTls() {
    delegate.disableTls();
  }

  @Override
  public EndpointAvailabilityHandler addRequestHandler(Collection<String> methods, String path, RequestHandler requestHandler) {
    return new EndpointAvailabilityHandlerWrapper(delegate.addRequestHandler(methods, path,
                                                                             new RequestHandlerWrapper(requestHandler)));
  }

  @Override
  public EndpointAvailabilityHandler addRequestHandler(String path, RequestHandler requestHandler) {
    return new EndpointAvailabilityHandlerWrapper(delegate.addRequestHandler(path, new RequestHandlerWrapper(requestHandler)));
  }

  @Override
  public SseEndpointManager sse(String ssePath, Consumer<SseRequestContext> onRequest, Consumer<SseClient> onClient) {
    // TODO: Hi!
    return null;
  }
}
