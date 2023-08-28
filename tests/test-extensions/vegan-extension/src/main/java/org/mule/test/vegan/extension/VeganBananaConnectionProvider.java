/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.tck.testmodels.fruit.Banana;

@Alias("BananaPlantain")
public class VeganBananaConnectionProvider implements ConnectionProvider<Banana> {

  @Parameter
  @Optional(defaultValue = "Ecuador")
  String originCountry;

  @Override
  public Banana connect() throws ConnectionException {
    Banana banana = new Banana();
    banana.setOrigin(originCountry);
    return banana;
  }

  @Override
  public void disconnect(Banana banana) {

  }

  @Override
  public ConnectionValidationResult validate(Banana banana) {
    return ConnectionValidationResult.success();
  }
}
