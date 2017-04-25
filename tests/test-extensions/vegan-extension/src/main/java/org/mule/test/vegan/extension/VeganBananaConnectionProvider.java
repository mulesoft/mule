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
