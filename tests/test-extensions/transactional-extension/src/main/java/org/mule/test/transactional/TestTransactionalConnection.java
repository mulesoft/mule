/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.connectivity.TransactionalConnection;

public class TestTransactionalConnection implements TransactionalConnection {

  private boolean begun, commited, rolledback = false;
  private boolean connected = true;

  @Override
  public void begin() throws Exception {
    begun = true;
  }

  @Override
  public void commit() throws Exception {
    commited = true;
  }

  @Override
  public void rollback() throws Exception {
    rolledback = true;
  }

  public boolean isTransactionBegun() {
    return begun;
  }

  public boolean isTransactionCommited() {
    return commited;
  }

  public boolean isTransactionRolledback() {
    return rolledback;
  }

  public void disconnect() {
    connected = false;
  }

  public boolean isConnected() {
    return connected;
  }
}
