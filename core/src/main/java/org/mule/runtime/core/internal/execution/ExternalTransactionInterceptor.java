/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import javax.transaction.TransactionManager;

public class ExternalTransactionInterceptor<T> implements ExecutionInterceptor<T> {

  private final ExecutionInterceptor<T> next;
  private TransactionConfig transactionConfig;
  private String appName;
  private NotificationDispatcher notificationDispatcher;
  private TransactionManager transactionManager;

  public ExternalTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig,
                                        String appName, NotificationDispatcher notificationDispatcher,
                                        TransactionManager transactionManager) {
    this.next = next;
    this.transactionConfig = transactionConfig;
    this.appName = appName;
    this.notificationDispatcher = notificationDispatcher;
    this.transactionManager = transactionManager;
  }

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    Transaction joinedExternal = null;
    Transaction tx = TransactionCoordination.getInstance().getTransaction();
    try {
      if (tx == null && transactionManager != null && transactionConfig != null && transactionConfig.isInteractWithExternal()) {

        TransactionFactory tmFactory = transactionConfig.getFactory();
        if (tmFactory instanceof ExternalTransactionAwareTransactionFactory) {
          ExternalTransactionAwareTransactionFactory externalTransactionFactory =
              (ExternalTransactionAwareTransactionFactory) tmFactory;
          joinedExternal = externalTransactionFactory.joinExternalTransaction(appName, notificationDispatcher,
                                                                              transactionManager);
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
