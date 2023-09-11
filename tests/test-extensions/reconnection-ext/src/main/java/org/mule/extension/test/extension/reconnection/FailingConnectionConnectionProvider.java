/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
