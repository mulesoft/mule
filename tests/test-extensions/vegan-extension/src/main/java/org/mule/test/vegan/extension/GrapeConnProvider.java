/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
