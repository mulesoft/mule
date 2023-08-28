/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.sdk.api.connectivity.NoConnectivityTest;

@Alias("kiwi")
public class VeganKiwiConnectionProvider implements ConnectionProvider<Kiwi>, NoConnectivityTest {

  @Override
  public Kiwi connect() throws ConnectionException {
    return new Kiwi();
  }

  @Override
  public void disconnect(Kiwi kiwi) {

  }

  @Override
  public ConnectionValidationResult validate(Kiwi kiwi) {
    return ConnectionValidationResult.success();
  }
}
