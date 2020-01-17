/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;


/**
 * This class (as it's name implies) provides connection instances and the funcionality to disconnect and validate those
 * connections.
 * <p>
 * All connection related parameters (values required in order to create a connection) must be
 * declared in the connection providers.
 * <p>
 * This particular example is a {@link PoolingConnectionProvider} which declares that connections resolved by this provider
 * will be pooled and reused. There are other implementations like {@link CachedConnectionProvider} which lazily creates and
 * caches connections or simply {@link ConnectionProvider} if you want a new connection each time something requires one.
 */
public class ReconnectableConnectionProvider implements CachedConnectionProvider<ReconnectableConnection> {

  public static volatile boolean fail;
  public static volatile int closePagingProviderCalls = 0;
  private int reconnectionAttempts = 0;
  private int disconnectCalls = 0;

  @Override
  public ReconnectableConnection connect() throws ConnectionException {
    if (fail) {
      reconnectionAttempts++;
      if (reconnectionAttempts <= 3) {
        throw new ConnectionException("FAAAAIL");
      }
      fail = false;
    }

    ReconnectableConnection connection =
        new ReconnectableConnection(reconnectionAttempts, disconnectCalls, closePagingProviderCalls);
    reconnectionAttempts = 0;

    return connection;
  }

  @Override
  public void disconnect(ReconnectableConnection connection) {
    disconnectCalls++;
  }

  @Override
  public ConnectionValidationResult validate(ReconnectableConnection connection) {
    return success();
  }
}
