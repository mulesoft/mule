/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionCoordinationSuspended extends TransactionCoordination {

  private TransactionCoordinationSuspended() {
    super();
  }

  private static final TransactionCoordinationSuspended instance = new TransactionCoordinationSuspended();

  public static TransactionCoordinationSuspended getInstance() {
    return instance;
  }


  static final Logger logger = LoggerFactory.getLogger(TransactionCoordinationSuspended.class);

  private final ThreadLocal<Deque<TransactionSuspended>> suspendedTransaction = new ThreadLocal<>();
  private final ThreadLocal<Deque<TransactionSuspended>> isolatedTransactions = new ThreadLocal<>();

  public boolean runningNestedTransaction() {
    Deque<TransactionSuspended> suspended = suspendedTransaction.get();
    return suspended != null && suspended.size() >= 1;
  }

  public void resumeXaTransactionIfAvailable() {
    try {
      if (suspendedTransaction.get() != null && suspendedTransaction.get().peek() != null) {
        resumeSuspendedTransaction();
      }
    } catch (TransactionException e) {
      logger.error("Failure resuming suspended transaction", e);
    }
  }

  public void suspendCurrentTransaction() throws TransactionException {
    TransactionSuspended tx = (TransactionSuspended) TransactionCoordinationSuspended.getInstance().getTransaction();
    if (logger.isDebugEnabled()) {
      logger.debug("Suspending " + tx);
    }

    tx.suspend();

    if (logger.isDebugEnabled()) {
      logger.debug("Successfully suspended " + tx);
      logger.debug("Unbinding the following TX from the current context: " + tx);
    }

    TransactionCoordination.getInstance().unbindTransaction(tx);
    if (suspendedTransaction.get() == null) {
      suspendedTransaction.set(new ArrayDeque<>());
    }
    suspendedTransaction.get().push(tx);
  }

  public void resumeSuspendedTransaction() throws TransactionException {
    TransactionSuspended tx = (suspendedTransaction.get() == null) ? null : suspendedTransaction.get().pop();
    if (logger.isDebugEnabled()) {
      logger.debug("Re-binding and Resuming " + tx);
    }
    TransactionCoordinationSuspended.getInstance().bindTransaction(tx);
    tx.resume();
  }

  public void clear() {
    if (suspendedTransaction.get() != null) {
      suspendedTransaction.get().clear();
    }
    suspendedTransaction.remove();
    transactions.remove();
    if (isolatedTransactions.get() != null) {
      isolatedTransactions.get().clear();
    }
    isolatedTransactions.remove();
  }

  public void isolateTransaction() {
    Transaction currentTransaction = transactions.get();
    if (currentTransaction != null) {
      if (isolatedTransactions.get() == null) {
        isolatedTransactions.set(new ArrayDeque<>());
      }
      isolatedTransactions.get().push((TransactionSuspended) transactions.get());
      transactions.set(null);
    }
  }

  public void restoreIsolatedTransaction() {
    if (isolatedTransactions.get() != null && !isolatedTransactions.get().isEmpty()) {
      transactions.set(isolatedTransactions.get().pop());
    }
  }
}
