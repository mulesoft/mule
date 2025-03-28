/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.client;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.util.function.Consumer;

/**
 * Configuration for a server-sent events source.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @since 4.9.3, 4.10.0
 */
@Experimental
public class SseSourceConfig {

  private final String url;
  private final SseRetryConfig retryConfig;
  private final Consumer<HttpRequestBuilder> requestCustomizer;
  private final HttpRequestOptions requestOptions;
  private final boolean preserveHeaderCase;

  public static SseSourceConfigBuilder builder(String url) {
    return new SseSourceConfigBuilder(url);
  }

  SseSourceConfig(String url, SseRetryConfig retryConfig, Consumer<HttpRequestBuilder> requestCustomizer,
                  HttpRequestOptions requestOptions, boolean preserveHeaderCase) {
    this.url = url;
    this.retryConfig = retryConfig;
    this.requestCustomizer = requestCustomizer;
    this.requestOptions = requestOptions;
    this.preserveHeaderCase = preserveHeaderCase;
  }

  /**
   * @return the url of the server.
   */
  public String getUrl() {
    return url;
  }

  /**
   * @return the {@link SseRetryConfig}.
   */
  public SseRetryConfig getRetryConfig() {
    return retryConfig;
  }

  /**
   * @return a callback to configure the initiator request.
   */
  public Consumer<HttpRequestBuilder> getRequestCustomizer() {
    return requestCustomizer;
  }

  /**
   * @return the request options of the initiator request.
   */
  public HttpRequestOptions getRequestOptions() {
    return requestOptions;
  }

  /**
   * @return whether the initiator request should preserve headers' case or not.
   * @since 4.10.0, 4.9.4
   */
  public boolean isPreserveHeaderCase() {
    return preserveHeaderCase;
  }
}
