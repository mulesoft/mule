/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import org.mule.runtime.http2.api.exception.RequestHandlerAlreadyPresentException;

import java.io.IOException;

/**
 * Represents an HTTP/2 Server. Notice it should be started to be bound, stopped to be unbound and finally disposed to release all
 * related components.
 *
 * @since 4.5
 */
public interface Http2Server {

  /**
   * Binds the underlying socket to the network interface and starts listening for requests.
   *
   * @return this server
   * @throws IOException          if there was a problem binding to the host and port specified.
   * @throws InterruptedException if the thread that was trying to bind to the host was interrupted.
   */
  Http2Server start() throws IOException, InterruptedException;

  /**
   * Unbinds the underlying socket from the network interface and stops listening for requests.
   * <p>
   * Currently executing requests are not affected and may proceed normally until {@link #dispose()} is called.
   *
   * @return this server
   */
  Http2Server stop();

  /**
   * Removes all secondary data to get rid of the server. Cannot be undone.
   * <p>
   * If there are any currently running requests, this method will block until either those requests are finished or a timeout
   * elapses. The requests that were received but didn't start processing, will be finished returning a 503 status code.
   */
  void dispose();

  /**
   * Adds a {@link Http2RequestHandler} on the given path and for all methods.
   *
   * @param path           the path to match.
   * @param requestHandler the handler to execute upon a matching request.
   * @return the {@link Http2RequestHandler} added.
   */
  // TODO: Should the path be a new class instead of an String?
  Http2RequestHandler addRequestHandler(final String path, final Http2RequestHandler requestHandler)
      throws RequestHandlerAlreadyPresentException;

  /**
   * Removes the {@link Http2RequestHandler} for the given path.
   *
   * @param path the path of the handler to be removed.
   */
  // TODO: Should it throw an exception when not found?
  void removeRequestHandler(String path);
}
