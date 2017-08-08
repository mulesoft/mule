/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Object that sends an HTTP request, and returns the response. Notice must be started to be used and stopped to be disposed
 * properly. Blocking and non-blocking options are available to execute requests.
 *
 * @since 4.0
 */
public interface HttpClient {

  /**
   * Fully configures the client, leaving it ready to use. Must be executed before any requests are attempted.
   */
  void start();

  /**
   * Disables the client. Must be executed to dispose all client related resources.
   */
  void stop();

  /**
   * Sends a HttpRequest blocking the current thread until a response is available or the request times out.
   *
   * @param request the {@link HttpRequest} to send
   * @param responseTimeout the time (in milliseconds) to wait for a response
   * @param followRedirects whether or not to follow redirect responses
   * @param authentication the optional {@link HttpAuthentication} to use
   * @return the received {@link HttpResponse}
   * @throws IOException if an error occurs while executing
   * @throws TimeoutException if {@code responseTimeout} is exceeded
   */
  HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpAuthentication authentication)
      throws IOException, TimeoutException;

  /**
   * Sends a HttpRequest without blocking the current thread. When a response is available or the request times out the returned
   * {@link CompletableFuture} will be completed. Be aware that the response body processing will be deferred so that the response can
   * be processed even when a large body is still being received. If the full response is needed right away then the provided
   * {@link HttpResponse} must be read in a different thread so that it does not block the {@link HttpClient} threads handling the
   * response. It's therefore recommended to use {@link CompletableFuture#get()} or any of the async methods available, such as
   * {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}, to handle the response is those scenarios since they guarantee
   * executing on a different thread.
   *
   * @param request the {@link HttpRequest} to send
   * @param responseTimeout the time (in milliseconds) to wait for a response
   * @param followRedirects whether or not to follow redirect responses
   * @param authentication the optional {@link HttpAuthentication} to use
   * @return a {@link CompletableFuture} that will complete once the {@link HttpResponse} is available
   */
  CompletableFuture<HttpResponse> sendAsync(HttpRequest request, int responseTimeout, boolean followRedirects,
                                            HttpAuthentication authentication);

}
