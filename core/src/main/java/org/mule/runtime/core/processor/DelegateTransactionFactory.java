/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.transaction.TransactionType.LOCAL;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TypedTransactionFactory;
import org.mule.runtime.core.transaction.TransactionType;

/**
 * Transaction factory for DelegateTransaction. Used for transactional element since transaction type is not known until the first
 * transactional message processor is executed.
 */
public class DelegateTransactionFactory implements TypedTransactionFactory {

  @Override
  public Transaction beginTransaction(MuleContext muleContext) throws TransactionException {
    DelegateTransaction delegateTransaction = new DelegateTransaction(muleContext);
    delegateTransaction.begin();
    return delegateTransaction;
  }

  @Override
  public boolean isTransacted() {
    return true;
  }

  @Override
  public TransactionType getType() {
    return LOCAL;
  }

}
