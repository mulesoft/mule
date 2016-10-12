/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

public class TestTransactionalConnection implements TransactionalConnection {

  private boolean transactionBegun, transactionCommited, transactionRolledback = false;
  private boolean connected = true;

  @Override
  public void begin() throws Exception {
    transactionBegun = true;
  }

  @Override
  public void commit() throws Exception {
    transactionCommited = true;
  }

  @Override
  public void rollback() throws Exception {
    transactionRolledback = true;
  }

  public boolean isTransactionBegun() {
    return transactionBegun;
  }

  public boolean isTransactionCommited() {
    return transactionCommited;
  }

  public boolean isTransactionRolledback() {
    return transactionRolledback;
  }

  public void disconnect() {
    connected = false;
  }

  public boolean isConnected() {
    return connected;
  }
}
