/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Alias("grape-connection")
public class GrapeConnProvider implements ConnectionProvider<VeganPolicy> {

  @NullSafe
  @Parameter
  @Optional
  public VeganPolicy veganPolicy;

  @Override
  public VeganPolicy connect() throws ConnectionException {
    return veganPolicy;
  }

  @Override
  public void disconnect(VeganPolicy connection) {

  }

  @Override
  public ConnectionValidationResult validate(VeganPolicy connection) {
    return success();
  }
}
