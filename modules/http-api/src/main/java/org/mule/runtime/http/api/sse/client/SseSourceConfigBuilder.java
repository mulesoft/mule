/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.client;

import static org.mule.runtime.http.api.sse.client.SseRetryConfig.defaultConfig;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.util.function.Consumer;

/**
 * Builder for instances of {@link SseSourceConfig}.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @since 4.9.3, 4.10.0
 */
@Experimental
public class SseSourceConfigBuilder {

  private final String url;
  private SseRetryConfig retryConfig = defaultConfig();
  private Consumer<HttpRequestBuilder> requestCustomizer = b -> {
  };
  private HttpRequestOptions requestOptions = HttpRequestOptions.builder().build();

  public SseSourceConfigBuilder(String url) {
    this.url = url;
  }

  public SseSourceConfigBuilder withRetryConfig(SseRetryConfig retryConfig) {
    this.retryConfig = retryConfig;
    return this;
  }

  public SseSourceConfigBuilder withRequestCustomizer(Consumer<HttpRequestBuilder> requestCustomizer) {
    this.requestCustomizer = requestCustomizer;
    return this;
  }

  public SseSourceConfigBuilder withRequestOptions(HttpRequestOptions requestOptions) {
    this.requestOptions = requestOptions;
    return this;
  }

  public SseSourceConfig build() {
    return new SseSourceConfig(url, retryConfig, requestCustomizer, requestOptions);
  }
}
