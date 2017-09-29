/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeginAndResolveTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private static final Logger logger = LoggerFactory.getLogger(BeginAndResolveTransactionInterceptor.class);
  private final ExecutionInterceptor<T> next;
  private final TransactionConfig transactionConfig;
  private final MuleContext muleContext;
  private final boolean processOnException;
  private boolean mustResolveAnyTransaction;

  public BeginAndResolveTransactionInterceptor(ExecutionInterceptor next, TransactionConfig transactionConfig,
                                               MuleContext muleContext,
                                               boolean processOnException, boolean mustResolveAnyTransaction) {
    this.next = next;
    this.transactionConfig = transactionConfig;
    this.muleContext = muleContext;
    this.processOnException = processOnException;
    this.mustResolveAnyTransaction = mustResolveAnyTransaction;
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
      tx = transactionConfig.getFactory().beginTransaction(muleContext);
      // Timeout is a traversal attribute of all Transaction implementations.
      // Setting it up here for all of them rather than in every implementation.
      tx.setTimeout(timeout);
      resolveStartedTransaction = true;
      if (logger.isDebugEnabled()) {
        logger.debug("Transaction successfully started: " + tx);
      }
    }
    T result;
    try {
      result = next.execute(callback, executionContext);
      resolveTransactionIfRequired(resolveStartedTransaction);
      return result;
    } catch (MessagingException e) {
      if (processOnException) {
        resolveTransactionIfRequired(resolveStartedTransaction || mustResolveAnyTransaction);
      }
      throw e;
    }
  }

  private void resolveTransactionIfRequired(boolean mustResolveTransaction) throws TransactionException {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (mustResolveTransaction && transaction != null) {
      TransactionCoordination.getInstance().resolveTransaction();
    }
  }

}
