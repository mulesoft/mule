/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;

import javax.transaction.TransactionManager;

/**
 * <code>TransactionFactory</code> creates a transaction.
 */
@NoImplement
public interface TransactionFactory {

  /**
   * Create and begins a new transaction
   *
   * @return a new Transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param applicationName        will be part of the notification
   * @param notificationDispatcher allows the Mule container to fire notifications
   */
  Transaction beginTransaction(String applicationName, NotificationDispatcher notificationDispatcher,
                               TransactionManager transactionManager)
      throws TransactionException;

  /**
   * Determines whether this transaction factory creates transactions that are really transacted or if they are being used to
   * simulate batch actions, such as using Jms Client Acknowledge.
   */
  boolean isTransacted();
}
