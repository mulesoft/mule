/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.tck.testmodels.fruit.Peach;

public class VeganPeachConnectionProvider implements ConnectionProvider<Peach> {

  @Override
  public Peach connect() throws ConnectionException {
    return new Peach();
  }

  @Override
  public void disconnect(Peach peach) {

  }

  @Override
  public ConnectionValidationResult validate(Peach peach) {
    return ConnectionValidationResult.success();
  }
}
