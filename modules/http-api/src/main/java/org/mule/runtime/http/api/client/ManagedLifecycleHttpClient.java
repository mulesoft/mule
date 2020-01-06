/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Decorates a {@link HttpClient} so that invocations to {@link HttpClient#start()} and {@link HttpClient#stop()} have no effect.
 * <p>
 * This is useful (and recommended) for cases in which a given component owns an {@link HttpClient} but needs to expose said
 * client to other consumers. Since the lifecycle of the client should only be handled by the owner, it is good practice to use
 * this decorator whenever the client is exposed, to guarantee that faulty consumers don't interfere with its lifecycle.
 *
 * @since 4.3.0
 */
public final class ManagedLifecycleHttpClient implements HttpClient {

  private final HttpClient httpClient;

  /**
   * Creates a new instance
   *
   * @param httpClient the decorated client
   */
  public ManagedLifecycleHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Does nothing
   */
  @Override
  public void stop() {
    // Nothing to do. The lifecycle of this object is handled by whoever owns the client.
  }

  /**
   * Does nothing
   */
  @Override
  public void start() {
    // Nothing to do. The lifecycle of this object is handled by whoever owns the client.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CompletableFuture<HttpResponse> sendAsync(HttpRequest request, HttpRequestOptions options) {
    return httpClient.sendAsync(request, options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpResponse send(HttpRequest request, HttpRequestOptions options) throws IOException, TimeoutException {
    return httpClient.send(request, options);
  }
}
