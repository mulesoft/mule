/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.tx.TransactionHandle;
import org.mule.runtime.extension.api.tx.Transactional;

/**
 * Default implementation of {@link TransactionHandle}. Use this instance when an actual transaction <b>has</b> been
 * started
 */
public class DefaultTransactionHandle extends IdempotentTransactionHandle<TransactionalConnection> {

  /**
   * Creates a new instance
   *
   * @param connection the connection on which the transaction started
   */
  public DefaultTransactionHandle(TransactionalConnection connection) {
    super(connection, Transactional::commit, Transactional::rollback);
  }
}
