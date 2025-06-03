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
import org.mule.runtime.module.extension.api.http.message.HttpRequestBuilderWrapper;
import org.mule.runtime.module.extension.api.http.message.sdktomule.HttpResponseWrapper;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfig;
import org.mule.sdk.api.http.domain.message.request.HttpRequestBuilder;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.sse.client.SseSourceConfig;

import java.util.function.Consumer;

public class SseSourceConfigImpl implements SseSourceConfig {

  private HttpResponse response;

  private String url;
  private boolean allowRetryDelayOverride;
  private long initialRetryDelayMillis;
  private boolean shouldRetryOnStreamEnd;
  private Consumer<HttpRequestBuilder> requestCustomizer;
  private Consumer<HttpRequestOptionsConfig> requestOptionsConfigConsumer;
  private boolean preserveHeadersCase;

  @Override
  public SseSourceConfig withResponse(HttpResponse response) {
    this.response = response;
    return this;
  }

  @Override
  public SseSourceConfig withUrl(String url) {
    this.url = url;
    return this;
  }

  @Override
  public SseSourceConfig withAllowRetryDelayOverride(boolean allow) {
    this.allowRetryDelayOverride = allow;
    return this;
  }

  @Override
  public SseSourceConfig withInitialRetryDelayMillis(long initialRetryDelayMillis) {
    this.initialRetryDelayMillis = initialRetryDelayMillis;
    return this;
  }

  @Override
  public SseSourceConfig withRetry(boolean shouldRetryOnStreamEnd) {
    this.shouldRetryOnStreamEnd = shouldRetryOnStreamEnd;
    return this;
  }

  @Override
  public SseSourceConfig withRequestCustomizer(Consumer<HttpRequestBuilder> requestCustomizer) {
    this.requestCustomizer = requestCustomizer;
    return this;
  }

  @Override
  public SseSourceConfig withRequestOptions(Consumer<HttpRequestOptionsConfig> requestOptions) {
    this.requestOptionsConfigConsumer = requestOptions;
    return this;
  }

  @Override
  public SseSourceConfig withPreserveHeadersCase(boolean preserveHeadersCase) {
    this.preserveHeadersCase = preserveHeadersCase;
    return this;
  }

  public org.mule.runtime.http.api.sse.client.SseSourceConfig build() {
    if (response == null) {
      var optionsBuilder = HttpRequestOptions.builder();
      var optionsConfigurer = new HttpRequestOptionsConfigToBuilder(optionsBuilder);
      requestOptionsConfigConsumer.accept(optionsConfigurer);

      return org.mule.runtime.http.api.sse.client.SseSourceConfig
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
