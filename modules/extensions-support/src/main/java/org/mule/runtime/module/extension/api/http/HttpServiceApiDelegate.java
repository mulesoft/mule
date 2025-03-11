/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.HttpService;
import org.mule.sdk.api.http.HttpServiceApi;
import org.mule.sdk.api.http.sse.ClientWithSse;
import org.mule.sdk.api.http.sse.ServerSentEventSource;
import org.mule.sdk.api.http.sse.ServerWithSse;
import org.mule.sdk.api.http.sse.SseClient;
import org.mule.sdk.api.http.sse.SseEndpointManager;
import org.mule.sdk.api.http.sse.SseRetryConfig;

import java.util.function.Consumer;

/**
 * Definition of {@link HttpServiceApi} that just delegates all to the {@link HttpService}.
 */
public class HttpServiceApiDelegate implements HttpServiceApi {

  @Override
  public SseEndpointManager sseEndpoint(ServerWithSse httpServer, String ssePath, Consumer<SseClient> sseClientHandler) {
    return httpServer.sse(ssePath, sseClientHandler);
  }

  @Override
  public ServerSentEventSource sseSource(ClientWithSse httpClient, String url, SseRetryConfig retryConfig) {
    return httpClient.sseSource(url, retryConfig);
  }
}
