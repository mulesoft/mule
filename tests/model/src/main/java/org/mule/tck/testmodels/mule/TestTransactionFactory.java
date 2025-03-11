/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

/**
 * <code>TestTransactionFactory</code> creates a {@link org.mule.tck.testmodels.mule.TestTransaction}
 */

public class TestTransactionFactory implements TransactionFactory {

  // for testing properties
  private String value;
  private Transaction mockTransaction;
  private boolean isXa;

  public TestTransactionFactory() {
    this(false);
  }

  public TestTransactionFactory(boolean isXa) {
    this.isXa = isXa;
  }

  public TestTransactionFactory(Transaction mockTransaction) {
    this.mockTransaction = mockTransaction;
  }

  @Override
  public Transaction beginTransaction(String applicationName, NotificationDispatcher notificationFirer)
      throws TransactionException {
    Transaction testTransaction;
    if (mockTransaction != null) {
      testTransaction = mockTransaction;
    } else {
      testTransaction = new TestTransaction(applicationName, notificationFirer, isXa);
    }

    testTransaction.begin();
    return testTransaction;
  }

  public boolean isTransacted() {
    return true;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
