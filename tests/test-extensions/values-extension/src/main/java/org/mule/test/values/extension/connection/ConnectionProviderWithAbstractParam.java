/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.values.extension.AbstractPojo;

@Alias("abstract-param-provider")
public class ConnectionProviderWithAbstractParam implements ConnectionProvider<AbstractParamConnection> {

  @Parameter
  private AbstractPojo pojo;

  @Override
  public AbstractParamConnection connect() throws ConnectionException {
    return new AbstractParamConnection(pojo.getName());
  }

  @Override
  public void disconnect(AbstractParamConnection abstractParamConnection) {
    // Nothing to do
  }

  @Override
  public ConnectionValidationResult validate(AbstractParamConnection abstractParamConnection) {
    return success();
  }

}
