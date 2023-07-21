/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import java.util.UUID;

public class FailingConnection {

  UUID uuid;

  public FailingConnection() {
    uuid = UUID.randomUUID();
  }

  public void send(String message) {
    // do nothing
  }

  public void sendWithFailure(String message) {
    throw new RuntimeException("Failure sending message " + message);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    return this.uuid.equals(((FailingConnection) o).uuid);
  }
}
