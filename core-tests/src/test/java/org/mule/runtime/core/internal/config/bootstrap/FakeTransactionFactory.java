/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.UniversalTransactionFactory;

import javax.transaction.TransactionManager;

public final class FakeTransactionFactory implements UniversalTransactionFactory {

  @Override
  public Transaction beginTransaction(MuleContext muleContext) {
    return null;
  }

  @Override
  public Transaction beginTransaction(String applicationName, NotificationDispatcher notificationFirer,
                                      SingleResourceTransactionFactoryManager transactionFactoryManager,
                                      TransactionManager transactionManager) {
    return null;
  }

  @Override
  public boolean isTransacted() {
    return false;
  }

  @Override
  public Transaction createUnboundTransaction(MuleContext muleContext) {
    return null;
  }

}
