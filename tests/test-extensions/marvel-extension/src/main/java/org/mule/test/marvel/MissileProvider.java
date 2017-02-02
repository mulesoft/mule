/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.test.marvel.model.Missile;

@Alias(("missile"))
public class MissileProvider implements ConnectionProvider<Missile> {

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
