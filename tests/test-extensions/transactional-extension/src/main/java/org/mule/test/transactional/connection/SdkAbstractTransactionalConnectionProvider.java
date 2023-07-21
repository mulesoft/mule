/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

abstract class SdkAbstractTransactionalConnectionProvider implements ConnectionProvider<SdkTestTransactionalConnection> {

  private DummyXaResource dummyXaResource = new DummyXaResource();

  @Parameter
  @Optional(defaultValue = "false")
  boolean useXa;

  @Override
  public SdkTestTransactionalConnection connect() throws ConnectionException {
    return useXa ? new SdkTestXaTransactionalConnection(dummyXaResource) : new SdkTestLocalTransactionalConnection();
  }

  @Override
  public void disconnect(SdkTestTransactionalConnection testTransactionalConnection) {
    testTransactionalConnection.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(SdkTestTransactionalConnection testTransactionalConnection) {
    return success();
  }

}
