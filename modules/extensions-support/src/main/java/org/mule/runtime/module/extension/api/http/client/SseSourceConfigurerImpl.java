/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import static org.mule.runtime.http.api.sse.client.SseSourceConfig.fromResponse;

import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.sse.client.SseRetryConfig;
import org.mule.runtime.http.api.sse.client.SseSourceConfig;
import org.mule.runtime.module.extension.api.http.message.HttpRequestBuilderWrapper;
import org.mule.runtime.module.extension.api.http.message.sdktomule.HttpResponseWrapper;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfigurer;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.sse.client.SseSourceConfigurer;

import java.util.function.Consumer;

public class SseSourceConfigurerImpl implements SseSourceConfigurer {

  private HttpResponse response;

  private String url;
  private boolean allowRetryDelayOverride;
  private long initialRetryDelayMillis;
  private boolean shouldRetryOnStreamEnd;
  private Consumer<HttpRequestBuilder> requestCustomizer;
  private Consumer<HttpRequestOptionsConfigurer> requestOptionsConfigConsumer;
  private boolean preserveHeadersCase;

  @Override
  public SseSourceConfigurer withResponse(HttpResponse response) {
    this.response = response;
    return this;
  }

  @Override
  public SseSourceConfigurer withUrl(String url) {
    this.url = url;
    return this;
  }

  @Override
  public SseSourceConfigurer withAllowRetryDelayOverride(boolean allow) {
    this.allowRetryDelayOverride = allow;
    return this;
  }

  @Override
  public SseSourceConfigurer withInitialRetryDelayMillis(long initialRetryDelayMillis) {
    this.initialRetryDelayMillis = initialRetryDelayMillis;
    return this;
  }

  @Override
  public SseSourceConfigurer withRetry(boolean shouldRetryOnStreamEnd) {
    this.shouldRetryOnStreamEnd = shouldRetryOnStreamEnd;
    return this;
  }

  @Override
  public SseSourceConfigurer withRequestCustomizer(Consumer<HttpRequestBuilder> requestCustomizer) {
    this.requestCustomizer = requestCustomizer;
    return this;
  }

  @Override
  public SseSourceConfigurer withRequestOptions(Consumer<HttpRequestOptionsConfigurer> requestOptions) {
    this.requestOptionsConfigConsumer = requestOptions;
    return this;
  }

  @Override
  public SseSourceConfigurer withPreserveHeadersCase(boolean preserveHeadersCase) {
    this.preserveHeadersCase = preserveHeadersCase;
    return this;
  }

  public SseSourceConfig build() {
    if (response == null) {
      var optionsBuilder = HttpRequestOptions.builder();
      var optionsConfigurer = new HttpRequestOptionsConfigurerToBuilder(optionsBuilder);
      requestOptionsConfigConsumer.accept(optionsConfigurer);

      return SseSourceConfig
          .builder(url)
          .withRetryConfig(new SseRetryConfig(allowRetryDelayOverride, initialRetryDelayMillis, shouldRetryOnStreamEnd))
          .withRequestCustomizer(b -> requestCustomizer.accept(new HttpRequestBuilderWrapper(b)))
          .withRequestOptions(optionsBuilder.build())
          .withPreserveHeadersCase(preserveHeadersCase)
          .build();
    } else {
      return fromResponse(new HttpResponseWrapper(response)).build();
    }
  }
}
