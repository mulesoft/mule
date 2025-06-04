/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.sdk.api.http.domain.message.request.HttpRequestContext;
import org.mule.sdk.api.http.sse.server.SseClientConfig;
import org.mule.sdk.api.http.sse.server.SseResponseCustomizer;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class SseClientConfigImpl implements SseClientConfig {

  private String clientId;
  private InetSocketAddress remoteAddress;
  private Consumer<SseResponseCustomizer> responseCustomizer;

  @Override
  public SseClientConfig withRequestContext(HttpRequestContext requestContext) {
    return withRemoteAddress(nullSafeGetAddress(requestContext));
  }

  @Override
  public SseClientConfig withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public SseClientConfig withRemoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public SseClientConfig customizeResponse(Consumer<SseResponseCustomizer> callback) {
    this.responseCustomizer = callback;
    return this;
  }

  public org.mule.runtime.http.api.sse.server.SseClientConfig build() {
    return org.mule.runtime.http.api.sse.server.SseClientConfig.builder()
        .withClientId(clientId)
        .withRemoteAddress(remoteAddress)
        .customizeResponse(customizer -> responseCustomizer.accept(new SseResponseCustomizerWrapper(customizer)))
        .build();
  }

  private static InetSocketAddress nullSafeGetAddress(HttpRequestContext requestContext) {
    if (requestContext == null) {
      return null;
    }
    var clientConnection = requestContext.getClientConnection();
    if (clientConnection == null) {
      return null;
    }
    return clientConnection.getRemoteHostAddress();
  }
}
