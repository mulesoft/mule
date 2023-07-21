/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.sdk.api.connectivity.TransactionalConnection;

public interface SdkTestTransactionalConnection extends TransactionalConnection {

  double getConnectionId();

  boolean isTransactionBegun();

  boolean isTransactionCommited();

  boolean isTransactionRolledback();

  void disconnect();

  boolean isConnected();
}
