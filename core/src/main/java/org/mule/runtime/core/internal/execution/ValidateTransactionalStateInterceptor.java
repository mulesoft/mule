/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

public class ValidateTransactionalStateInterceptor<T> implements ExecutionInterceptor<T> {

  private final ExecutionInterceptor<T> next;
  private final TransactionConfig transactionConfig;

  public ValidateTransactionalStateInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig) {
    this.next = next;
    this.transactionConfig = transactionConfig;
  }

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    Transaction tx = TransactionCoordination.getInstance().getTransaction();
    if (transactionConfig.getAction() == TransactionConfig.ACTION_NEVER && tx != null) {
      throw new IllegalTransactionStateException(CoreMessages.transactionAvailableButActionIs("Never"));
    } else if (transactionConfig.getAction() == TransactionConfig.ACTION_ALWAYS_JOIN && tx == null) {
      throw new IllegalTransactionStateException(CoreMessages.transactionNotAvailableButActionIs("Always Join"));
    }
    return this.next.execute(callback, executionContext);
  }
}
