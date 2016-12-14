/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;


/**
 * <code>ExternalTransactionAwareTransactionFactory</code> creates a transaction, possibly representing a transaction started
 * outside Mule.
 *
 */
public interface ExternalTransactionAwareTransactionFactory extends TransactionFactory {

  /**
   * Create and begins a new transaction
   *
   * @return a new Transaction representing an existing external transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param muleContext
   */
  Transaction joinExternalTransaction(MuleContext muleContext) throws TransactionException;
}
