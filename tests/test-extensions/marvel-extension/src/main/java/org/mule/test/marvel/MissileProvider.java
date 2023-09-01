/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.DIRECTORY;
import static org.mule.test.marvel.MissileProvider.NAME;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.file.FilePath;
import org.mule.test.marvel.model.Missile;

@Alias(NAME)
public class MissileProvider implements ConnectionProvider<Missile> {

  public static final String NAME = "missile";

  @Parameter
  @Optional
  @FilePath(type = DIRECTORY)
  private String missileConfigurationDirectory;

  @Override
  public Missile connect() throws ConnectionException {
    return new Missile();
  }

  @Override
  public void disconnect(Missile missile) {
    missile.setArmed(false);
  }

  @Override
  public ConnectionValidationResult validate(Missile connection) {
    return success();
  }
}
