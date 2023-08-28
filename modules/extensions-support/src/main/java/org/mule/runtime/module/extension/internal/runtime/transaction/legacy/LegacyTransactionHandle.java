/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction.legacy;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.tx.TransactionHandle;

/**
 * Adapts a {@link org.mule.sdk.api.tx.TransactionHandle} into a legacy {@link TransactionHandle}
 *
 * @since 4.4.0
 */
public class LegacyTransactionHandle implements TransactionHandle {

  private final org.mule.sdk.api.tx.TransactionHandle delegate;

  public LegacyTransactionHandle(org.mule.sdk.api.tx.TransactionHandle delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean isTransacted() {
    return delegate.isTransacted();
  }

  @Override
  public void commit() throws TransactionException {
    delegate.commit();
  }

  @Override
  public void rollback() throws TransactionException {
    delegate.rollback();
  }

  @Override
  public void resolve() throws TransactionException {
    delegate.resolve();
  }
}
