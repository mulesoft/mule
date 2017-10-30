/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution.compatibility;

import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.execution.ExecutionContext;
import org.mule.runtime.core.internal.execution.ExecutionInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ExecutionInterceptor} implementation to exclusively be used for Compatibility purposes.
 */
public class ResolvePreviousTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private static final Logger logger = LoggerFactory.getLogger(ResolvePreviousTransactionInterceptor.class);
  final private ExecutionInterceptor<T> next;
  private TransactionConfig transactionConfig;

  public ResolvePreviousTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig) {
    this.next = next;
    this.transactionConfig = transactionConfig;
  }

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    byte action = transactionConfig.getAction();
    Transaction transactionBeforeTemplate = TransactionCoordination.getInstance().getTransaction();
    if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
        && transactionBeforeTemplate != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(action + ", " + "current TX: " + transactionBeforeTemplate);
      }

      resolveTransaction();
    }
    return next.execute(callback, executionContext);
  }

  protected void resolveTransaction() throws TransactionException {
    TransactionCoordination.getInstance().resolveTransaction();
  }
}
