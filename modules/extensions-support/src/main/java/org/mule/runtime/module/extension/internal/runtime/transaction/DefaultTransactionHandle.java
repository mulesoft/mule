/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.tx.TransactionHandle;

/**
 * Default {@link TransactionHandle} implementation.
 * <p>
 * This will execute a transaction action only is the transaction is still alive.
 *
 * @since 4.0
 */
public class DefaultTransactionHandle implements TransactionHandle {

  /**
   * {@inheritDoc}
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
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction != null) {
      transaction.commit();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws TransactionException {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction != null) {
      transaction.rollback();
    }
  }

  @Override
  public void resolve() throws TransactionException {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction != null) {
      TransactionCoordination.getInstance().resolveTransaction();
    }
  }
}
