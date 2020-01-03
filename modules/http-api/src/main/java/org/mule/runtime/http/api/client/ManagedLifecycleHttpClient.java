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

public final class ManagedLifecycleHttpClient implements HttpClient {

  private final HttpClient httpClient;

  public ManagedLifecycleHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void stop() {
    // Nothing to do. The lifecycle of this object is handled by whoever owns the client.
  }

  @Override
  public void start() {
    // Nothing to do. The lifecycle of this object is handled by whoever owns the client.
  }

  @Override
  public CompletableFuture<HttpResponse> sendAsync(HttpRequest request, HttpRequestOptions options) {
    return httpClient.sendAsync(request, options);
  }

  @Override
  public HttpResponse send(HttpRequest request, HttpRequestOptions options) throws IOException, TimeoutException {
    return httpClient.send(request, options);
  }
}
