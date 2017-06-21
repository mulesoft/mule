/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

public class ExternalTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private final ExecutionInterceptor<T> next;
  private TransactionConfig transactionConfig;
  private MuleContext muleContext;

  public ExternalTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig,
                                        MuleContext muleContext) {
    this.next = next;
    this.transactionConfig = transactionConfig;
    this.muleContext = muleContext;
  }

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    Transaction joinedExternal = null;
    Transaction tx = TransactionCoordination.getInstance().getTransaction();
    try {
      if (tx == null && muleContext != null && transactionConfig != null && transactionConfig.isInteractWithExternal()) {

        TransactionFactory tmFactory = transactionConfig.getFactory();
        if (tmFactory instanceof ExternalTransactionAwareTransactionFactory) {
          ExternalTransactionAwareTransactionFactory externalTransactionFactory =
              (ExternalTransactionAwareTransactionFactory) tmFactory;
          joinedExternal = externalTransactionFactory.joinExternalTransaction(muleContext);
        }
      }
      return next.execute(callback, executionContext);
    } finally {
      if (joinedExternal != null) {
        TransactionCoordination.getInstance().unbindTransaction(joinedExternal);
      }
    }
  }
}
