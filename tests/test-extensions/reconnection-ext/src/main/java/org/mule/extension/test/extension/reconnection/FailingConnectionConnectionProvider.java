/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("failing-connection")
public class FailingConnectionConnectionProvider implements CachedConnectionProvider<FailingConnection> {

  @Override
  public FailingConnection connect() throws ConnectionException {
    return new FailingConnection();
  }

  @Override
  public void disconnect(FailingConnection connection) {
    // do nothing
  }

  @Override
  public ConnectionValidationResult validate(FailingConnection connection) {
    return failure("Connection failure", new ConnectionException("Connection failure"));
  }
}
