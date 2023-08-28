/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
