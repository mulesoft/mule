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
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

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
  private final HttpResponse response;

  /**
   * Creates a builder that allows you to configure certain parameters of the request and the retry mechanism. Use this method if
   * you want the source to connect to a server with a certain URL and send the request to establish the SSE communication with
   * auto-retry.
   *
   * @param url the server URL.
   * @return a builder to configure the source parameters.
   */
  public static SseSourceConfigBuilder fromUrl(String url) {
    return new SseSourceConfigBuilder(url);
  }

  /**
   * Creates a builder useful when you already have an {@link HttpResponse} with an SSE stream. The resulting SSE Source won't do
   * automatic retries.
   *
   * @param response a response that has a {@code text/event-stream} as payload.
   * @return a builder to create the {@link SseSourceConfig}.
   */
  public static SseSourceConfigBuilderFromResponse fromResponse(HttpResponse response) {
    return new SseSourceConfigBuilderFromResponse(response);
  }

  SseSourceConfig(String url, SseRetryConfig retryConfig, Consumer<HttpRequestBuilder> requestCustomizer,
                  HttpRequestOptions requestOptions, boolean preserveHeaderCase, HttpResponse response) {
    this.url = url;
    this.retryConfig = retryConfig;
    this.requestCustomizer = requestCustomizer;
    this.requestOptions = requestOptions;
    this.preserveHeaderCase = preserveHeaderCase;
    this.response = response;
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

  /**
   * @return a response whose content is a stream of events ({@code text/event-stream}).
   * @since 4.10.0, 4.9.5
   */
  public HttpResponse getResponse() {
    return response;
  }
}
