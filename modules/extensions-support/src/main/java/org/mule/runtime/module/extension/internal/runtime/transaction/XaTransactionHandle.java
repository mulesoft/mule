/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.extension.api.tx.TransactionHandle;

import javax.transaction.TransactionManager;

/**
 * {@link TransactionHandle} implementation for XA Transactions
 *
 * @since 4.0
 */
public class XaTransactionHandle extends IdempotentTransactionHandle<TransactionManager> {

  /**
   * Creates a new {@link XaTransactionHandle} instance for a given {@link TransactionManager}
   *
   * @param transactionManager {@link TransactionManager} to be used for the transaction actions.
   */
  public XaTransactionHandle(TransactionManager transactionManager) {
    super(transactionManager, TransactionManager::commit, TransactionManager::rollback);
  }
}
