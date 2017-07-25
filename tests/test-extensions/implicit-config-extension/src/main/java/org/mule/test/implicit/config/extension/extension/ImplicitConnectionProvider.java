/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.implicit.config.extension.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class ImplicitConnectionProvider implements ConnectionProvider<Counter> {

  @Parameter
  @Optional(defaultValue = "#[vars.number]")
  private int number;

  @Override
  public Counter connect() throws ConnectionException {
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
