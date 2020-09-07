/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.connection;

import static org.mule.sdk.api.connectivity.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.ConnectionValidationResult;
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
