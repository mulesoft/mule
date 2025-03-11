/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.HttpClient;
import org.mule.sdk.api.http.sse.ServerSentEventSource;
import org.mule.sdk.api.http.sse.SseRetryConfig;

import java.util.concurrent.CompletableFuture;

public class HttpClientWrapper implements HttpClient<HttpRequest, HttpRequestOptions, HttpResponse> {

  private final org.mule.runtime.http.api.client.HttpClient delegate;

  public HttpClientWrapper(org.mule.runtime.http.api.client.HttpClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableFuture<HttpResponse> sendAsync(HttpRequest request, HttpRequestOptions httpRequestOptions) {
    return delegate.sendAsync(request, httpRequestOptions);
  }

  @Override
  public ServerSentEventSource sseSource(String url, SseRetryConfig retryConfig) {
    return delegate.sseSource(url, retryConfig);
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }
}
