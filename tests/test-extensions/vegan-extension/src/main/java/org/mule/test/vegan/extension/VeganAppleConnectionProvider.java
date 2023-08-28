/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.tck.testmodels.fruit.Apple;

@Alias("apple")
public class VeganAppleConnectionProvider implements ConnectionProvider<Apple>, NoConnectivityTest {

  @Override
  public Apple connect() throws ConnectionException {
    return new Apple();
  }

  @Override
  public void disconnect(Apple apple) {

  }

  @Override
  public ConnectionValidationResult validate(Apple apple) {
    return ConnectionValidationResult.success();
  }
}
