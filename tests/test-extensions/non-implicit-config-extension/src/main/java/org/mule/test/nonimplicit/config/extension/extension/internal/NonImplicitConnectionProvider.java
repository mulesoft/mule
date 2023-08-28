/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.nonimplicit.config.extension.extension.internal;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.nonimplicit.config.extension.extension.api.Counter;

public class NonImplicitConnectionProvider implements ConnectionProvider<Counter> {

  @ParameterGroup(name = "Internal")
  private PrivateParameterGroup privateGroup;

  @Parameter
  @Optional(defaultValue = "#[vars.number]")
  private int number;

  @Override
  public Counter connect() throws ConnectionException {
    if (privateGroup == null) {
      throw new ConnectionException("Internal parameter group was not instantiate with nullsafe");
    }
    return new Counter(number);
  }

  @Override
  public void disconnect(Counter counter) {

  }

  @Override
  public ConnectionValidationResult validate(Counter counter) {
    return ConnectionValidationResult.success();
  }
}
