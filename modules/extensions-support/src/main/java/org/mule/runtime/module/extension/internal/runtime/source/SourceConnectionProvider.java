/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Bridges a {@link SourceConnectionManager} to the {@link ConnectionProvider} contract
 *
 * @since 4.0
 */
public class SourceConnectionProvider implements ConnectionProvider<Object> {

  private final SourceConnectionManager connectionManager;
  private final ConfigurationInstance config;

  public SourceConnectionProvider(SourceConnectionManager connectionManager, ConfigurationInstance config) {
    this.connectionManager = connectionManager;
    this.config = config;
  }

  @Override
  public Object connect() throws ConnectionException {
    return connectionManager.getConnection(config);
  }

  @Override
  public void disconnect(Object connection) {
    connectionManager.release(connection);
  }

  @Override
  public ConnectionValidationResult validate(Object connection) {
    return connectionManager.testConnectivity(connection);
  }
}
