/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionFactory;

import javax.transaction.TransactionManager;

/**
 * Creates instances of {@link ExtensionTransactionFactory}
 *
 * @since 4.0
 */
public class ExtensionTransactionFactory implements TransactionFactory {

  @Override
  public Transaction beginTransaction(MuleContext muleContext) throws TransactionException {
    Transaction transaction = new ExtensionTransaction(muleContext);
    transaction.begin();

    return transaction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transaction beginTransaction(String applicationName, NotificationDispatcher notificationFirer,
                                      SingleResourceTransactionFactoryManager transactionFactoryManager,
                                      TransactionManager transactionManager, int timeout)
      throws TransactionException {
    Transaction transaction = new ExtensionTransaction(applicationName, notificationFirer, timeout);
    transaction.begin();

    return transaction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTransacted() {
    return true;
  }
}
