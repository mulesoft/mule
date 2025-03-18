/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.client;

import static org.mule.runtime.http.api.sse.client.SseRetryConfig.defaultConfig;

import static java.util.Objects.requireNonNull;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.util.function.Consumer;

/**
 * Builder for instances of {@link SseSourceConfig}. The url is required, so you have to specify it in the constructor of the
 * builder, and the rest of the parameters have their own with* method.
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
    this.url = requireNonNull(url);
  }

  /**
   * Configures the {@link SseRetryConfig}.
   * 
   * @param retryConfig the retry config.
   * @return this same builder.
   */
  public SseSourceConfigBuilder withRetryConfig(SseRetryConfig retryConfig) {
    this.retryConfig = retryConfig;
    return this;
  }

  /**
   * Configures a callback to customize the initiator request builder.
   * 
   * @param requestCustomizer the callback.
   * @return this same builder.
   */
  public SseSourceConfigBuilder withRequestCustomizer(Consumer<HttpRequestBuilder> requestCustomizer) {
    this.requestCustomizer = requestCustomizer;
    return this;
  }

  /**
   * Configures the {@link HttpRequestOptions} of the initiator request.
   * 
   * @param requestOptions the request options.
   * @return this same builder.
   */
  public SseSourceConfigBuilder withRequestOptions(HttpRequestOptions requestOptions) {
    this.requestOptions = requestOptions;
    return this;
  }

  /**
   * Builds an instance of {@link SseSourceConfig} with the specified parameters. This method is intended to be called only once.
   * 
   * @return the new instance of {@link SseSourceConfig}.
   */
  public SseSourceConfig build() {
    return new SseSourceConfig(url, retryConfig, requestCustomizer, requestOptions);
  }
}
