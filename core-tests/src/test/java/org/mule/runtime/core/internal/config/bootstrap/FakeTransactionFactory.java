/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
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
