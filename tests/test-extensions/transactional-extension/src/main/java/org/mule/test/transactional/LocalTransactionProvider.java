/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;

public class LocalTransactionProvider implements PoolingConnectionProvider<TestTransactionalConnection> {

  @Override
  public TestTransactionalConnection connect() throws ConnectionException {
    return new TestTransactionalConnection();
  }

  @Override
  public void disconnect(TestTransactionalConnection testTransactionalConnection) {
    testTransactionalConnection.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(TestTransactionalConnection testTransactionalConnection) {
    return success();
  }
}
