/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.secure;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Text;

public class SecureConnectionProvider implements ConnectionProvider<Object> {

  @Parameter
  @Password
  private String providerPassword;

  @Parameter
  @Text
  private String anotherLongText;

  @Override
  public Object connect() throws ConnectionException {
    return null;
  }

  @Override
  public void disconnect(Object o) {

  }

  @Override
  public ConnectionValidationResult validate(Object o) {
    return ConnectionValidationResult.success();
  }
}
