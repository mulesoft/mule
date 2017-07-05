/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
