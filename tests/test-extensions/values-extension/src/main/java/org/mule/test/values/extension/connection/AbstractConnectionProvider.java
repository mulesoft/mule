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
