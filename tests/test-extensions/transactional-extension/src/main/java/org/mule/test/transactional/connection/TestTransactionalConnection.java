/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

public interface TestTransactionalConnection extends TransactionalConnection {

  double getConnectionId();

  boolean isTransactionBegun();

  boolean isTransactionCommited();

  boolean isTransactionRolledback();

  void disconnect();

  boolean isConnected();
}
