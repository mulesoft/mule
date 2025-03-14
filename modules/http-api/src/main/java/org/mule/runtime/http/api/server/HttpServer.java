/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpConstants.Protocol;
import org.mule.runtime.http.api.server.ws.WebSocketHandler;
import org.mule.runtime.http.api.server.ws.WebSocketHandlerManager;
import org.mule.runtime.http.api.sse.server.SseClient;
import org.mule.runtime.http.api.sse.server.SseEndpointManager;
import org.mule.runtime.http.api.sse.server.SseRequestContext;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Represents a ServerSocket connection. Notice it should be started to be bound, stopped to be unbound and finally disposed to
 * release all related components. To extends it's functionality and not depend on API changes, this object should be used
 * internally instead of decorated.
 *
 * @since 4.0
 */
@NoImplement
public interface HttpServer {

  /**
   * Binds the ServerSocket to the network interface and starts listening for requests.
   *
   * @return this server
   * @throws IOException if there was a problem binding to the host and port specified.
   */
  HttpServer start() throws IOException;

  /**
   * Unbinds the ServerSocket to the network interface and stops listening for requests.
   * <p>
   * Currently executing requests are not affected and may proceed normally until {@link #dispose()} is called.
   *
   * @return this server
   */
  HttpServer stop();

  /**
   * Removes all secondary data to get rid of the server. Cannot be undone.
   * <p>
   * If there are any currently running requests, this method will block until either those requests are finished or a timeout
   * elapses. The requests that were received but didn't start processing, will be finished returning a 503 status code.
   */
  void dispose();

  /**
   * @return the server address this server is listening on.
   */
  ServerAddress getServerAddress();

  /**
   * @return the protocol (HTTP or HTTPS) this server is expecting.
   */
  Protocol getProtocol();

  /**
   * @return true if the server is currently stopping, false if it's stopped already or if it's not doing stop.
   */
  boolean isStopping();

  /**
   * @return true if the server is stopped, false otherwise.
   */
  boolean isStopped();

  /**
   * Enable TLS dynamically on this server using the supplied TLS context factory
   *
   * @param tlsContextFactory The TLS context factory to be used when dynamically enabled TLS at this server
   */
  default void enableTls(TlsContextFactory tlsContextFactory) {
    throw new UnsupportedOperationException("TLS is not supported");
  }

  /**
   * Disable TLS dynamically. It should work for when the TLS was dynamically enabled or even when the TLS was statically
   * configured in the listener configuration section of the application.
   */
  default void disableTls() {
    throw new UnsupportedOperationException("TLS is not supported");
  }

  /**
   * Adds a {@link RequestHandler} on the given path and for the given methods.
   *
   * @param methods        a list of methods to match
   * @param path           the path to match
   * @param requestHandler the handler to execute upon a matching request
   * @return a {@link RequestHandlerManager} for the handler
   */
  RequestHandlerManager addRequestHandler(final Collection<String> methods, final String path,
                                          final RequestHandler requestHandler);

  /**
   * Adds a {@link RequestHandler} on the given path and for all methods.
   *
   * @param path           the path to match
   * @param requestHandler the handler to execute upon a matching request
   * @return a {@link RequestHandlerManager} for the handler
   */
  RequestHandlerManager addRequestHandler(final String path, final RequestHandler requestHandler);

  /**
   * Adds a {@link WebSocketHandler}
   *
   * @param handler the handler
   * @return a {@link WebSocketHandlerManager}
   * @since 4.2.0
   */
  default WebSocketHandlerManager addWebSocketHandler(WebSocketHandler handler) {
    throw new UnsupportedOperationException("WebSockets are only supported in Enterprise Edition");
  }

  /**
   * Adds an endpoint to produce server-sent events.
   * <p>
   * This API is EXPERIMENTAL. Do not use it until it is stable.
   *
   * @param ssePath   path to match.
   * @param onRequest callback to be executed when a request is received. It can be used to customize the SSE response and the SSE
   *                  Client to be created.
   * @param onClient  callback to be executed for each received {@link SseClient}. Note: this callback will be executed AFTER
   *                  {@code onRequest}.
   * @return an object that can be used to enable/disable/remove the endpoint from the server.
   *
   * @since 4.10.0
   */
  @Experimental
  default SseEndpointManager sse(String ssePath,
                                 Consumer<SseRequestContext> onRequest,
                                 Consumer<SseClient> onClient) {
    throw new UnsupportedOperationException("Server-sent Events (SSE) are not supported in this HTTP Service version");
  }

  /**
   * Adds an endpoint to produce server-sent events. Equivalent to call {@link #sse(String, Consumer, Consumer)} with a no-op
   * {@code onRequest} callback.
   * <p>
   * This API is EXPERIMENTAL. Do not use it until it is stable.
   *
   * @param ssePath          path to match.
   * @param sseClientHandler callback to be executed for each received {@link SseClient}.
   * @return an object that can be used to enable/disable/remove the endpoint from the server.
   *
   * @since 4.10.0
   */
  @Experimental
  default SseEndpointManager sse(String ssePath, Consumer<SseClient> sseClientHandler) {
    return sse(ssePath, ctx -> {
    }, sseClientHandler);
  }
}
