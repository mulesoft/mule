/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.tx.TransactionHandle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link TransactionHandle}. Use this instance when an actual transaction <b>has</b> been
 * started
 */
public class DefaultTransactionHandle implements TransactionHandle {

  private final TransactionalConnection connection;
  private final AtomicBoolean txResolved = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param connection the connection on which the transaction started
   */
  public DefaultTransactionHandle(TransactionalConnection connection) {
    this.connection = connection;
  }

  /**
   * {@inheritDoc}
   * @return {@code true}
   */
  @Override
  public boolean isTransacted() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws TransactionException {
    resolveTxAs(connection::commit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws TransactionException {
    resolveTxAs(connection::rollback);
  }

  private void resolveTxAs(CheckedRunnable task) {
    if (txResolved.compareAndSet(false, true)) {
      task.run();
      TransactionCoordination.getInstance().clear();
    }
  }
}
