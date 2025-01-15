/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.tls.TlsContextFactory;

import java.net.InetSocketAddress;

public class Http2ServerConfiguration {

  private InetSocketAddress serverAddress;
  private TlsContextFactory tlsContextFactory;

  public static Builder builder() {
    return new Builder();
  }

  private Http2ServerConfiguration() {}

  public InetSocketAddress getServerAddress() {
    return serverAddress;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
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

    public Builder withTlsContextFactory(TlsContextFactory tlsContextFactory) {
      product.tlsContextFactory = tlsContextFactory;
      return this;
    }

    public Http2ServerConfiguration build() {
      checkArgument(product.serverAddress != null, "Server address can't be null");
      return product;
    }
  }
}
