/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.test.values.extension.ValuesConnection;

public class AbstractConnectionProvider implements ConnectionProvider<ValuesConnection> {

  @Override
  public ValuesConnection connect() throws ConnectionException {
    return new ValuesConnection();
  }

  @Override
  public void disconnect(ValuesConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(ValuesConnection connection) {
    return ConnectionValidationResult.success();
  }

}
