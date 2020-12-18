/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.tx.TransactionHandle;

public class SdkToLegacyTransactionHandle implements TransactionHandle {

  private final org.mule.sdk.api.tx.TransactionHandle delegate;

  public SdkToLegacyTransactionHandle(org.mule.sdk.api.tx.TransactionHandle delegate) {
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
