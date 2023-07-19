/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

public class IsolateCurrentTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private ExecutionInterceptor<T> next;
  private TransactionConfig transactionConfig;

  public IsolateCurrentTransactionInterceptor(ExecutionInterceptor<T> nextProcessingInterceptor,
                                              TransactionConfig transactionConfig) {
    this.next = nextProcessingInterceptor;
    this.transactionConfig = transactionConfig;
  }


  @Override
  public T execute(ExecutionCallback<T> muleEventProcessingCallback, ExecutionContext executionContext) throws Exception {
    boolean transactionIsolated = false;
    try {
      if (transactionConfig.getAction() == TransactionConfig.ACTION_NOT_SUPPORTED) {
        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction != null) {
          TransactionCoordination.getInstance().isolateTransaction();
          transactionIsolated = true;
        }
      }
      return next.execute(muleEventProcessingCallback, executionContext);
    } finally {
      if (transactionIsolated) {
        TransactionCoordination.getInstance().restoreIsolatedTransaction();
      }
    }
  }
}
