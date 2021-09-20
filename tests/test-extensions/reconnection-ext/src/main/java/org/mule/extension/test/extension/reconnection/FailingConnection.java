/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
