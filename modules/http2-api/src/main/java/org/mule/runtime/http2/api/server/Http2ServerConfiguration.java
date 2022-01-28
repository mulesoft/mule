/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import java.net.InetSocketAddress;

public class Http2ServerConfiguration {

  private InetSocketAddress socketAddress;

  public static Builder builder() {
    return new Builder();
  }

  private Http2ServerConfiguration() {}

  public InetSocketAddress getSocketAddress() {
    return socketAddress;
  }

  public static class Builder {

    private Http2ServerConfiguration product;

    private Builder() {
      product = new Http2ServerConfiguration();
    }

    public Builder withSocketAddress(InetSocketAddress socketAddress) {
      product.socketAddress = socketAddress;
      return this;
    }

    public Http2ServerConfiguration build() {
      return product;
    }
  }
}
