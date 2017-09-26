/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction.xa;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.transaction.ExternalXaTransaction;
import org.mule.runtime.core.privileged.transaction.XaTransaction;

import javax.transaction.TransactionManager;

/**
 * <code>XaTransactionFactory</code> Is used to create/retrieve a Transaction from a transaction manager configured on the
 * MuleManager.
 */
public class XaTransactionFactory implements ExternalTransactionAwareTransactionFactory {

  private int timeout;

  public Transaction beginTransaction(MuleContext muleContext) throws TransactionException {
    try {
      XaTransaction xat = new XaTransaction(muleContext);
      xat.setTimeout(timeout);
      xat.begin();
      return xat;
    } catch (Exception e) {
      throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
    }
  }

  /**
   * Create a Mule transaction that represents a transaction started outside of Mule
   */
  public Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException {
    try {
      TransactionManager txManager = muleContext.getTransactionManager();
      if (txManager.getTransaction() == null) {
        return null;
      }
      XaTransaction xat = new ExternalXaTransaction(muleContext);
      xat.begin();
      return xat;
    } catch (Exception e) {
      throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
    }
  }

  /**
   * Determines whether this transaction factory creates transactions that are really transacted or if they are being used to
   * simulate batch actions, such as using Jms Client Acknowledge.
   */
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
