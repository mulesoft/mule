/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server;

import java.io.IOException;
import java.util.Collection;

/**
 * Represents a ServerSocket connection.
 *
 * @since 4.0
 */
public interface HttpServer {

  /**
   * Binds the ServerSocket to the network interface and starts listening for requests
   * 
   * @throws IOException if there was a problem binding to the host and port specified
   */
  void start() throws IOException;

  /**
   * Unbinds the ServerSocket to the network interface and stops listening for requests.
   */
  void stop();

  /**
   * Removes all secondary data to get rid of the server.
   */
  void dispose();

  /**
   * @return the server address this server is listening on
   */
  ServerAddress getServerAddress();

  /**
   * @return true if the server is currently stopping, false if it's stopped already or if it's not doing stop.
   */
  boolean isStopping();

  /**
   * @return true if the server is stopped, false otherwise
   */
  boolean isStopped();

  RequestHandlerManager addRequestHandler(final Collection<String> methods, final String path,
                                          final RequestHandler requestHandler);

  RequestHandlerManager addRequestHandler(final String path, final RequestHandler requestHandler);
}
