/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;

import javax.transaction.TransactionManager;

/**
 * <code>TransactionFactory</code> creates a transaction.
 */
public interface TransactionFactory {

  /**
   * Create and begins a new transaction. If you use this method please set the timeout in the next step.
   * 
   * @return a new Transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param muleContext
   *
   * @deprecated since 4.3.0. Use
   *             {@link #beginTransaction(String, NotificationDispatcher, SingleResourceTransactionFactoryManager, TransactionManager)}
   *             instead
   */
  @Deprecated
  Transaction beginTransaction(MuleContext muleContext) throws TransactionException;

  /**
   * Create and begins a new transaction
   *
   * @return a new Transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param applicationName        will be part of the notification
   * @param notificationDispatcher allows the Mule container to fire notifications
   */
  default Transaction beginTransaction(String applicationName, NotificationDispatcher notificationDispatcher,
                                       SingleResourceTransactionFactoryManager transactionFactoryManager,
                                       TransactionManager transactionManager)
      throws TransactionException {
    Transaction transaction = beginTransaction(((DefaultNotificationDispatcher) notificationDispatcher).getContext());
    return transaction;
  }

  /**
   * Determines whether this transaction factory creates transactions that are really transacted or if they are being used to
   * simulate batch actions, such as using Jms Client Acknowledge.
   */
  boolean isTransacted();
}
