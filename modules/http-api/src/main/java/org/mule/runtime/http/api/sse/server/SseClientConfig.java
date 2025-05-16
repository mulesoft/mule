/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.server;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;

import java.net.InetSocketAddress;

/**
 * Configuration for the creation of the {@link SseClient}.
 *
 * @since 4.10.0, 4.9.5
 */
@Experimental
public class SseClientConfig {

  public static SseClientConfigBuilder builderFrom(HttpRequestContext requestContext) {
    return builder().withRemoteAddress(nullSafeGetAddress(requestContext));
  }

  public static SseClientConfigBuilder builder() {
    return new SseClientConfigBuilder();
  }

  private final String clientId;
  private final InetSocketAddress remoteAddress;

  SseClientConfig(String clientId, InetSocketAddress remoteHostAddress) {
    this.clientId = clientId;
    this.remoteAddress = remoteHostAddress;
  }

  /**
   * @return the id that will be returned by {@link SseClient#getClientId()}. If nothing is configured, a default UUID will be
   *         calculated.
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * @return the address that will be returned by {@link SseClient#getRemoteAddress()}.
   */
  public InetSocketAddress getRemoteHostAddress() {
    return remoteAddress;
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
