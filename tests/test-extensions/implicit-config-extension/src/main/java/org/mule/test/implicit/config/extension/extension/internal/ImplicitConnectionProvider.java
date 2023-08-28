/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.config.extension.extension.internal;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.implicit.config.extension.extension.api.Counter;

public class ImplicitConnectionProvider implements ConnectionProvider<Counter> {

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
