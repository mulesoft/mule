/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.support.internal.message.muletosdk.HttpResponseWrapper;
import org.mule.runtime.http.support.internal.message.sdktomule.HttpRequestWrapper;
import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfig;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.sse.client.SseSource;
import org.mule.sdk.api.http.sse.client.SseSourceConfig;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class HttpClientWrapper implements HttpClient {

  private final org.mule.runtime.http.api.client.HttpClient delegate;

  public HttpClientWrapper(org.mule.runtime.http.api.client.HttpClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableFuture<HttpResponse> sendAsync(HttpRequest request,
                                                   Consumer<HttpRequestOptionsConfig> configurerConsumer) {
    var builder = HttpRequestOptions.builder();
    var configurer = new HttpRequestOptionsConfigToBuilder(builder);
    configurerConsumer.accept(configurer);
    var options = builder.build();

    var future = new CompletableFuture<HttpResponse>();
    delegate.sendAsync(new HttpRequestWrapper(request), options).whenComplete((res, err) -> {
      if (err != null) {
        future.completeExceptionally(err);
      } else {
        future.complete(new HttpResponseWrapper(res));
      }
    });
    return future;
  }

  @Override
  public SseSource sseSource(Consumer<SseSourceConfig> configConsumer) {
    SseSourceConfigImpl configurer = new SseSourceConfigImpl();
    configConsumer.accept(configurer);
    return new SseSourceWrapper(delegate.sseSource(configurer.build()));
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
