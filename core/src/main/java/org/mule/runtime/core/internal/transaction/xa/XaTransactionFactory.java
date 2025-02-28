/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction.xa;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotStartTransaction;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.internal.transaction.XaTransaction;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

import javax.transaction.TransactionManager;

/**
 * <code>XaTransactionFactory</code> Is used to create/retrieve a Transaction from a transaction manager configured on the
 * MuleManager.
 */
@NoExtend
public class XaTransactionFactory implements TransactionFactory {

  private int timeout;

  @Override
  public Transaction beginTransaction(String applicationName, NotificationDispatcher notificationFirer,
                                      TransactionManager transactionManager)
      throws TransactionException {
    try {
      XaTransaction xat = new XaTransaction(applicationName, transactionManager, notificationFirer);
      xat.setTimeout(timeout);
      xat.begin();
      return xat;
    } catch (Exception e) {
      throw new TransactionException(cannotStartTransaction("XA"), e);
    }
  }

  /**
   * Determines whether this transaction factory creates transactions that are really transacted or if they are being used to
   * simulate batch actions, such as using Jms Client Acknowledge.
   */
  @Override
  public boolean isTransacted() {
    return true;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
