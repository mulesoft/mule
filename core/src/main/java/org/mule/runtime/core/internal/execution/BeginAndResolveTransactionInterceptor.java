/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.transaction.TransactionAdapter;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeginAndResolveTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private static final Logger logger = LoggerFactory.getLogger(BeginAndResolveTransactionInterceptor.class);
  private final ExecutionInterceptor<T> next;
  private final MuleTransactionConfig transactionConfig;
  private final String applicationName;
  private final NotificationDispatcher notificationDispatcher;
  private final boolean processOnException;
  private final boolean mustResolveAnyTransaction;
  private final boolean errorAtTimeout;

  public BeginAndResolveTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig,
                                               String applicationName, NotificationDispatcher notificationDispatcher,
                                               boolean processOnException, boolean mustResolveAnyTransaction,
                                               boolean errorAtTimeout) {
    this.next = next;
    this.transactionConfig = (MuleTransactionConfig) transactionConfig;
    this.applicationName = applicationName;
    this.notificationDispatcher = notificationDispatcher;
    this.processOnException = processOnException;
    this.mustResolveAnyTransaction = mustResolveAnyTransaction;
    this.errorAtTimeout = errorAtTimeout;
  }

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    byte action = transactionConfig.getAction();
    int timeout = transactionConfig.getTimeout();

    boolean resolveStartedTransaction = false;
    Transaction tx = TransactionCoordination.getInstance().getTransaction();
    if (action == TransactionConfig.ACTION_ALWAYS_BEGIN || (action == TransactionConfig.ACTION_BEGIN_OR_JOIN && tx == null)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Beginning transaction");
      }
      executionContext.markTransactionStart();

      // Timeout is a traversal attribute of all Transaction implementations.
      // Setting it up here for all of them rather than in every implementation.
      tx = transactionConfig.getFactory().beginTransaction(applicationName,
                                                           notificationDispatcher);
      tx.setTimeout(timeout);
      if (tx instanceof TransactionAdapter txAdapter) {
        txAdapter.setRollbackIfTimeout(errorAtTimeout);
      }
      resolveStartedTransaction = true;
      logger.debug("Transaction successfully started: {}", tx);
    }
    T result;
    try {

      result = next.execute(callback, executionContext);
      resolveTransactionIfRequired(resolveStartedTransaction);
      return result;
    } catch (MessagingException e) {
      if (processOnException) {
        rollbackTransactionIfRequired(resolveStartedTransaction || mustResolveAnyTransaction);
      }
      throw e;
    }
  }

  private void rollbackTransactionIfRequired(boolean mustResolveTransaction) {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (mustResolveTransaction && transaction != null) {
      TransactionCoordination.getInstance().rollbackCurrentTransaction();
    }
  }

  private void resolveTransactionIfRequired(boolean mustResolveTransaction) throws TransactionException {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (mustResolveTransaction && transaction != null) {
      TransactionCoordination.getInstance().resolveTransaction();
    }
  }

}
