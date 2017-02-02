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
 * <code>TransactionFactory</code> creates a transaction.
 */
public interface TransactionFactory {

  /**
   * Create and begins a new transaction
   * 
   * @return a new Transaction
   * @throws TransactionException if the transaction cannot be created or begun
   * @param muleContext
   */
  Transaction beginTransaction(MuleContext muleContext) throws TransactionException;

  /**
   * Determines whether this transaction factory creates transactions that are really transacted or if they are being used to
   * simulate batch actions, such as using Jms Client Acknowledge.
   */
  boolean isTransacted();
}
