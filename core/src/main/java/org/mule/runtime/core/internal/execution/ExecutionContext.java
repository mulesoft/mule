/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
