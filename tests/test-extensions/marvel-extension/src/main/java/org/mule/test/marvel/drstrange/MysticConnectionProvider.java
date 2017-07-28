/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.marvel.ironman.IronMan;

@Alias("mystic")
public class MysticConnectionProvider implements ConnectionProvider<MysticConnection> {

  @ConfigReference(namespace = HEISENBERG, name = "config")
  @ConfigReference(namespace = MARVEL_EXTENSION, name = IronMan.CONFIG_NAME)
  @Parameter
  @Optional
  private String multipleConfig;

  @Override
  public MysticConnection connect() throws ConnectionException {
    return new MysticConnection();
  }

  @Override
  public void disconnect(MysticConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(MysticConnection connection) {
    return success();
  }
}
