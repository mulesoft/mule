/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.transaction.ExternalXaTransaction;
import org.mule.runtime.core.privileged.transaction.XaTransaction;

import javax.transaction.TransactionManager;

/**
 * <code>ExternalTransactionAwareTransactionFactory</code> creates a transaction, possibly representing a transaction started
 * outside Mule.
 *
 */
@NoImplement
public interface ExternalTransactionAwareTransactionFactory extends TransactionFactory {

  /**
   * Create and begins a new transaction
   *
   * @return a new Transaction representing an existing external transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param muleContext
   *
   * @deprecated since 4.2.3. Use {@link #joinExternalTransaction(String, TransactionManager, NotificationDispatcher, int)} instead
   *
   */
  @Deprecated
  Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException;

  /**
   * Create and begins a new transaction
   *
   * @return a new Transaction representing an existing external transaction
   * @throws TransactionException if the transaction cannot be created or begun
   */
  default Transaction joinExternalTransaction(String applicationName, TransactionManager transactionManager,
                                              NotificationDispatcher notificationFirer,
                                              int timeout)
      throws TransactionException {
    try {
      if (transactionManager.getTransaction() == null) {
        return null;
      }
      XaTransaction xat = new ExternalXaTransaction(applicationName, transactionManager, notificationFirer, timeout);
      xat.begin();
      return xat;
    } catch (Exception e) {
      throw new TransactionException(CoreMessages.cannotStartTransaction("External"), e);
    }
  }
}
