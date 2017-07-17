/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.runtime.http.api.HttpConstants.Protocol;

import java.io.IOException;
import java.util.Collection;

/**
 * Represents a ServerSocket connection.
 *
 * @since 4.0
 */
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
   *
   * @return this server
   */
  HttpServer stop();

  /**
   * Removes all secondary data to get rid of the server. Cannot be undone.
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
   * Adds a {@link RequestHandler} on the given path and for the given methods.
   *
   * @param methods a list of methods to match
   * @param path the path to match
   * @param requestHandler the handler to execute upon a matching request
   * @return a {@link RequestHandlerManager} for the handler
   */
  RequestHandlerManager addRequestHandler(final Collection<String> methods, final String path,
                                          final RequestHandler requestHandler);

  /**
   * Adds a {@link RequestHandler} on the given path and for all methods.
   *
   * @param path the path to match
   * @param requestHandler the handler to execute upon a matching request
   * @return a {@link RequestHandlerManager} for the handler
   */
  RequestHandlerManager addRequestHandler(final String path, final RequestHandler requestHandler);
}
