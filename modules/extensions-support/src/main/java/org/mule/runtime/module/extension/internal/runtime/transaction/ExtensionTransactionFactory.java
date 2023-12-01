/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotStartTransaction;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import javax.transaction.TransactionManager;

/**
 * Creates instances of {@link ExtensionTransactionFactory}
 *
 * @since 4.0
 */
public class ExtensionTransactionFactory implements TransactionFactory {

  @Override
  public Transaction beginTransaction(MuleContext muleContext) throws TransactionException {
    try {
      return this.beginTransaction(muleContext.getConfiguration().getId(),
                                   ((MuleContextWithRegistry) muleContext).getRegistry()
                                       .lookupObject(NotificationDispatcher.class),
                                   muleContext.getTransactionManager());
    } catch (RegistrationException e) {
      throw new TransactionException(cannotStartTransaction("Extension"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transaction beginTransaction(String applicationName, NotificationDispatcher notificationFirer,
                                      TransactionManager transactionManager)
      throws TransactionException {
    Transaction transaction = new ExtensionTransaction(applicationName, notificationFirer);
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
