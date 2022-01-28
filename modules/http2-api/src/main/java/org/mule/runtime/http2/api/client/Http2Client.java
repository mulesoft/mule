/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.client;

import org.mule.runtime.http2.api.domain.Http2Request;
import org.mule.runtime.http2.api.domain.Http2RequestOptions;
import org.mule.runtime.http2.api.domain.Http2Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Object that sends an HTTP/2 request, and returns the response. Notice it must be started to be used and stopped to be disposed
 * properly. Blocking and non-blocking options are available to execute requests.
 *
 * @since 4.5
 */
public interface Http2Client {

  /**
   * Fully configures the client, leaving it ready to use. Must be executed before any requests are attempted.
   */
  void start();

  /**
   * Disables the client. Must be executed to dispose all client related resources.
   */
  void stop();

  /**
   * Same as {@link #send(Http2Request, Http2RequestOptions)} but using default options.
   *
   * @param request the {@link Http2Request} to send
   * @return the received {@link Http2Response}
   * @throws IOException      if an error occurs while executing
   * @throws TimeoutException if {@code responseTimeout} is exceeded
   */
  default Http2Response send(Http2Request request) throws IOException, TimeoutException {
    return send(request, Http2RequestOptions.builder().build());
  }

  /**
   * Sends a {@link Http2Request} blocking the current thread until a response is available or the request times out.
   * 
   * @see #sendAsync(Http2Request, Http2RequestOptions) The non-blocking version.
   *
   * @param request the {@link Http2Request} to send
   * @param options the {@link Http2RequestOptions} to use
   * @return the received {@link Http2Response}
   * @throws IOException      if an error occurs while executing
   * @throws TimeoutException if {@code responseTimeout} is exceeded
   */
  Http2Response send(Http2Request request, Http2RequestOptions options) throws IOException, TimeoutException;

  /**
   * Same as {@link #sendAsync(Http2Request, Http2RequestOptions)} but using default options.
   *
   * @param request the {@link Http2Request} to send
   * @return a {@link CompletableFuture} that will complete once the {@link Http2Response} is available or an error occurred
   */
  default CompletableFuture<Http2Response> sendAsync(Http2Request request) {
    return sendAsync(request, Http2RequestOptions.builder().build());
  }

  /**
   * Sends a HttpRequest without blocking the current thread. When a response is available or the request times out the returned
   * {@link CompletableFuture} will be completed. Be aware that the response body processing will be deferred so that the response
   * can be processed even when a large body is still being received. If the full response is needed right away then the provided
   * {@link Http2Response} must be read in a different thread so that it does not block the {@link Http2Client} threads handling
   * the response. It's therefore recommendable to use {@link CompletableFuture#get()} or any of the async methods available, such
   * as {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}, to handle the response is those scenarios since they
   * guarantee executing on a different thread.
   *
   * @param request the {@link Http2Request} to send
   * @param options the {@link Http2RequestOptions} to use
   * @return a {@link CompletableFuture} that will complete once the {@link Http2Response} is available
   */
  CompletableFuture<Http2Response> sendAsync(Http2Request request, Http2RequestOptions options);
}
