/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

/**
 * This class represents an extension connection just as example (there is no real connection with anything here c:).
 */
public final class ReconnectableConnection {

  private final int reconnectionAttempts;
  private final int disconnectCalls;
  private final int closePagingProviderCalls;

  public ReconnectableConnection(int reconnectionAttempts, int disconnectCalls, int closePagingProviderCalls) {
    this.reconnectionAttempts = reconnectionAttempts;
    this.disconnectCalls = disconnectCalls;
    this.closePagingProviderCalls = closePagingProviderCalls;
  }

  public int getReconnectionAttempts() {
    return reconnectionAttempts;
  }

  public int getDisconnectCalls() {
    return disconnectCalls;
  }

  public int getClosePagingProviderCalls() {
    return closePagingProviderCalls;
  }
}
