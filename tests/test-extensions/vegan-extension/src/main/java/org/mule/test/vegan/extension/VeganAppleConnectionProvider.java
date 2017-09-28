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
