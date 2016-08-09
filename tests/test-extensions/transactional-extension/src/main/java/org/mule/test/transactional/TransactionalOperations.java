/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.extension.api.annotation.param.Connection;

public class TransactionalOperations {

  public TestTransactionalConnection getConnection(@Connection TestTransactionalConnection connection) {
    return connection;
  }

  public void verifyNoTransaction(@Connection TestTransactionalConnection connection) {
    checkState(!connection.isTransactionBegun(), "transaction begun with no reason");
  }

  public void verifyTransactionBegun(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionBegun(), "transaction not begun");
  }

  public void verifyTransactionCommited(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionCommited(), "transaction not committed");
  }

  public void verifyTransactionRolledback(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionRolledback(), "transaction not rolled back");
  }

  public void fail() {
    throw new RuntimeException("you better rollback!");
  }
}
