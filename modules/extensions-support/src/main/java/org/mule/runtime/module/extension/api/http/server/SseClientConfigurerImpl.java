/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.runtime.http.api.sse.server.SseClientConfig;
import org.mule.sdk.api.http.domain.message.request.HttpRequestContext;
import org.mule.sdk.api.http.sse.server.SseClientConfigurer;
import org.mule.sdk.api.http.sse.server.SseResponseCustomizer;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class SseClientConfigurerImpl implements SseClientConfigurer {

  private String clientId;
  private InetSocketAddress remoteAddress;
  private Consumer<SseResponseCustomizer> responseCustomizer;

  @Override
  public SseClientConfigurer withRequestContext(HttpRequestContext requestContext) {
    return withRemoteAddress(nullSafeGetAddress(requestContext));
  }

  @Override
  public SseClientConfigurer withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public SseClientConfigurer withRemoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public SseClientConfigurer customizeResponse(Consumer<SseResponseCustomizer> consumer) {
    this.responseCustomizer = consumer;
    return this;
  }

  public SseClientConfig build() {
    return SseClientConfig.builder()
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
