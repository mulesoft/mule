/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

@Alias("long-disconnection")
public class LongDisconnectionConnectionProvider implements PoolingConnectionProvider<SynchronizableConnection> {

  private static final int DISCONNECTION_TIMEOUT = 3;

  @Override
  public SynchronizableConnection connect() throws ConnectionException {
    return new SynchronizableConnection();
  }

  @Override
  public void disconnect(SynchronizableConnection synchronizableConnection) {
    try {
      boolean result = synchronizableConnection.getDisconnectionLatch()
          .await(DISCONNECTION_TIMEOUT, SECONDS);
      synchronizableConnection.setDisconnectionWaitedFullTimeout(!result);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ConnectionValidationResult validate(SynchronizableConnection synchronizableConnection) {
    return success();
  }
}
