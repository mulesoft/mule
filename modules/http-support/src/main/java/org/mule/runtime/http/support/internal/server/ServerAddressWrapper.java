/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.runtime.http.api.server.ServerAddress;

import java.net.InetAddress;

public class ServerAddressWrapper implements org.mule.sdk.api.http.server.ServerAddress {

  private final ServerAddress delegate;

  public ServerAddressWrapper(ServerAddress delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getPort() {
    return delegate.getPort();
  }

  @Override
  public String getIp() {
    return delegate.getIp();
  }

  @Override
  public InetAddress getAddress() {
    return delegate.getAddress();
  }

  @Override
  public boolean overlaps(org.mule.sdk.api.http.server.ServerAddress serverAddress) {
    if (serverAddress instanceof ServerAddressWrapper otherWrapper) {
      return delegate.overlaps(otherWrapper.delegate);
    } else {
      throw new IllegalArgumentException("Comparing pears with apples!");
    }
  }
}
