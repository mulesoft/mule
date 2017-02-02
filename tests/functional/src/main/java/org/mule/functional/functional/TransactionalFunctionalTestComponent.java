/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * This service is useful for unit tests involving transactionality because it will roll back the current transaction upon message
 * arrival.
 */
public class TransactionalFunctionalTestComponent extends FunctionalTestComponent {

  private boolean expectTransaction = true;
  private boolean rollback = true;

  /** {@inheritDoc} */
  @Override
  public Object onCall(MuleEventContext context) throws Exception {
    Object replyMessage = super.onCall(context);

    if (expectTransaction) {
      // Verify transaction has begun.
      Transaction currentTx = context.getCurrentTransaction();
      if (currentTx == null || !currentTx.isBegun()) {
        throw new TransactionException(I18nMessageFactory
            .createStaticMessage("Trying to roll back transaction but no transaction is underway."));
      }

      if (rollback) {
        // Mark the transaction for rollback.
        logger.info("@@@@ Rolling back transaction @@@@");
        currentTx.setRollbackOnly();
      }
    }

    return replyMessage;
  }

  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  public boolean isExpectTransaction() {
    return expectTransaction;
  }

  public void setExpectTransaction(boolean expectTransaction) {
    this.expectTransaction = expectTransaction;
  }
}

