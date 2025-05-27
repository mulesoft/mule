/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.client.HttpRequestOptionsBuilder;
import org.mule.runtime.module.extension.api.http.message.muletosdk.HttpResponseWrapper;
import org.mule.runtime.module.extension.api.http.message.sdktomule.HttpRequestWrapper;
import org.mule.sdk.api.http.client.HttpClient;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfigurer;
import org.mule.sdk.api.http.client.auth.HttpAuthentication;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class HttpClientWrapper implements HttpClient {

  private final org.mule.runtime.http.api.client.HttpClient delegate;

  public HttpClientWrapper(org.mule.runtime.http.api.client.HttpClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableFuture<HttpResponse> sendAsync(HttpRequest request,
                                                   Consumer<HttpRequestOptionsConfigurer> configurerConsumer) {
    var builder = HttpRequestOptions.builder();
    var configurer = new HttpRequestOptionsConfigurerToBuilder(builder);
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
  public void start() {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  private record HttpRequestOptionsConfigurerToBuilder(HttpRequestOptionsBuilder builder)
      implements HttpRequestOptionsConfigurer {

    @Override
    public HttpRequestOptionsConfigurer setResponseTimeout(int responseTimeout) {
      builder.responseTimeout(responseTimeout);
      return this;
    }

    @Override
    public HttpRequestOptionsConfigurer setFollowsRedirect(boolean followsRedirect) {
      builder.followsRedirect(followsRedirect);
      return this;
    }

    @Override
    public HttpRequestOptionsConfigurer setAuthentication(HttpAuthentication authentication) {
      // builder.authentication(authentication);
      return this;
    }

    @Override
    public HttpRequestOptionsConfigurer setProxyConfig(ProxyConfig proxyConfig) {
      // builder.proxyConfig(proxyConfig);
      return this;
    }

    @Override
    public HttpRequestOptionsConfigurer setSendBodyAlways(boolean sendBodyAlways) {
      builder.sendBodyAlways(sendBodyAlways);
      return this;
    }
  }
}
