/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.values.extension.ValuesConnection;

public class ValuesConnectionProvider implements ConnectionProvider<ValuesConnection> {

  @Parameter
  @Optional
  String someParameter;

  @Override
  public ValuesConnection connect() throws ConnectionException {
    return new ValuesConnection();
  }

  @Override
  public void disconnect(ValuesConnection optionsConnection) {

  }

  @Override
  public ConnectionValidationResult validate(ValuesConnection optionsConnection) {
    return ConnectionValidationResult.success();
  }
}
