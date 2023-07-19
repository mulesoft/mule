/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.transaction.TransactionCoordination;

/**
 * Provides information about the current execution of an {@link org.mule.runtime.core.api.execution.ExecutionTemplate}
 */
public class ExecutionContext {

  private boolean transactionStarted;

  /**
   * @return true if the current transaction must be resolved within the current context
   */
  public boolean needsTransactionResolution() {
    return transactionStarted && TransactionCoordination.getInstance().getTransaction() != null;
  }

  /**
   * This method must be called whenever a transaction has been created in the execution context
   */
  public void markTransactionStart() {
    this.transactionStarted = true;
  }

}
