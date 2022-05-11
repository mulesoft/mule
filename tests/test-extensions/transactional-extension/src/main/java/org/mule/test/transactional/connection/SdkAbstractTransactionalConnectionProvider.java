/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
