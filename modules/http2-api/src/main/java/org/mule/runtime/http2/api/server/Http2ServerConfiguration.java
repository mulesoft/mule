/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import static org.mule.runtime.app.declaration.internal.utils.Preconditions.checkArgument;

import java.net.InetSocketAddress;

public class Http2ServerConfiguration {

  private InetSocketAddress serverAddress;

  public static Builder builder() {
    return new Builder();
  }

  private Http2ServerConfiguration() {}

  public InetSocketAddress getServerAddress() {
    return serverAddress;
  }

  public static class Builder {

    private Http2ServerConfiguration product;

    private Builder() {
      product = new Http2ServerConfiguration();
    }

    public Builder withServerAddress(InetSocketAddress socketAddress) {
      product.serverAddress = socketAddress;
      return this;
    }

    public Http2ServerConfiguration build() {
      checkArgument(product.serverAddress != null, "Server address can't be null");
      return product;
    }
  }
}
