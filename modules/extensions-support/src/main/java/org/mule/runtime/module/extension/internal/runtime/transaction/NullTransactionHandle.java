/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.tx.TransactionHandle;

/**
 * Null implementation of {@link TransactionHandle}. Use in cases in which no transaciton is actually active
 */
public class NullTransactionHandle implements TransactionHandle {


  /**
   * {@inheritDoc}
   * @return {@code false}
   */
  @Override
  public boolean isTransacted() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws TransactionException {
    // no - op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws TransactionException {
    // no - op
  }
}
