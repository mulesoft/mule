/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.server;

import org.mule.api.annotation.Experimental;

import java.net.InetSocketAddress;

/**
 * Builder for instances of {@link SseClientConfig}.
 *
 * @since 4.10.0, 4.9.6
 */
@Experimental
public class SseClientConfigBuilder {

  private String clientId;
  private InetSocketAddress remoteAddress;

  public SseClientConfig build() {
    return new SseClientConfig(clientId, remoteAddress);
  }

  public SseClientConfigBuilder withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public SseClientConfigBuilder withRemoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }
}
