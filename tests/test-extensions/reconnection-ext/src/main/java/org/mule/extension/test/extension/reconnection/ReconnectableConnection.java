/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;


/**
 * This class represents an extension connection just as example (there is no real connection with anything here c:).
 */
public final class ReconnectableConnection {

  private final int reconnectionAttempts;

  public ReconnectableConnection(int reconnectionAttempts) {
    this.reconnectionAttempts = reconnectionAttempts;
  }

  public int getReconnectionAttempts() {
    return reconnectionAttempts;
  }

  @Override
  public String toString() {
    return "ReconnectableConnection: " + reconnectionAttempts;
  }
}
