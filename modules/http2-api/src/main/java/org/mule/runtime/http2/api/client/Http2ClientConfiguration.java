/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.client;

import java.net.InetSocketAddress;

public class Http2ClientConfiguration {

  private InetSocketAddress serverAddress;

  public static Builder builder() {
    return new Builder();
  }

  private Http2ClientConfiguration() {}

  public InetSocketAddress getServerAddress() {
    return serverAddress;
  }

  public static class Builder {

    private final Http2ClientConfiguration product;

    private Builder() {
      product = new Http2ClientConfiguration();
    }

    public Http2ClientConfiguration build() {
      return product;
    }

    public Builder withServerAddress(InetSocketAddress serverAddress) {
      product.serverAddress = serverAddress;
      return this;
    }
  }
}
