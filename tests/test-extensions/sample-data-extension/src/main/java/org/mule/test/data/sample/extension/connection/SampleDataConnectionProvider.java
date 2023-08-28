/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.connection;


import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.test.data.sample.extension.SampleDataConnection;

public class SampleDataConnectionProvider implements ConnectionProvider<SampleDataConnection> {

  @Override
  public SampleDataConnection connect() throws ConnectionException {
    return new SampleDataConnection();
  }

  @Override
  public void disconnect(SampleDataConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(SampleDataConnection connection) {
    return success();
  }
}
