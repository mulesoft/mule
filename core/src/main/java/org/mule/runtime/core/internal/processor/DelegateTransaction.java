/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.privileged.transaction.AbstractTransaction;

/**
 * Transaction placeholder to replace with proper transaction once transactional resource is discovered by mule
 */
public class DelegateTransaction extends AbstractTransaction {

  private Transaction delegate = new NullTransaction();

  public DelegateTransaction(MuleContext muleContext) {
    super(muleContext);
  }

  @Override
  protected void doBegin() throws TransactionException {}

  @Override
  protected void doCommit() throws TransactionException {
    delegate.commit();
  }

  @Override
  protected void doRollback() throws TransactionException {
    delegate.rollback();
  }

  @Override
  public int getStatus() throws TransactionException {
    return delegate.getStatus();
  }

  @Override
  public boolean isBegun() throws TransactionException {
    return delegate.isBegun();
  }

  @Override
  public boolean isRolledBack() throws TransactionException {
    return delegate.isRolledBack();
  }

  @Override
  public boolean isCommitted() throws TransactionException {
    return delegate.isCommitted();
  }

  @Override
  public Object getResource(Object key) {
    return delegate.getResource(key);
  }

  @Override
  public boolean hasResource(Object key) {
    return delegate.hasResource(key);
  }

  @Override
  public boolean supports(Object key, Object resource) {
    return delegate.supports(key, resource);
  }

  @Override
  public void bindResource(Object key, Object resource) throws TransactionException {
    if (!(this.delegate instanceof NullTransaction)) {
      throw new TransactionException(CoreMessages
          .createStaticMessage("Single resource transaction has already a resource bound"));
    }
    TransactionFactory transactionFactory = muleContext.getTransactionFactoryManager().getTransactionFactoryFor(key.getClass());
    this.unbindTransaction();
    int timeout = delegate.getTimeout();
    this.delegate = transactionFactory.beginTransaction(muleContext);
    delegate.setTimeout(timeout);
    delegate.bindResource(key, resource);
  }

  @Override
  public void setRollbackOnly() throws TransactionException {
    delegate.setRollbackOnly();
  }

  @Override
  public boolean isRollbackOnly() throws TransactionException {
    return delegate.isRollbackOnly();
  }

  @Override
  public boolean isXA() {
    return delegate.isXA();
  }

  @Override
  public void resume() throws TransactionException {
    delegate.resume();
  }

  @Override
  public javax.transaction.Transaction suspend() throws TransactionException {
    return delegate.suspend();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  public boolean supportsInnerTransaction(Transaction transaction) {
    return this.delegate instanceof NullTransaction || this.delegate == transaction;
  }

  @Override
  public int getTimeout() {
    return delegate.getTimeout();
  }

  @Override
  public void setTimeout(int timeout) {
    delegate.setTimeout(timeout);
  }

  private class NullTransaction implements Transaction {

    private int timeout = muleContext.getConfiguration().getDefaultTransactionTimeout();

    @Override
    public void begin() throws TransactionException {}

    @Override
    public void commit() throws TransactionException {}

    @Override
    public void rollback() throws TransactionException {}

    @Override
    public int getStatus() throws TransactionException {
      return Transaction.STATUS_UNKNOWN;
    }

    @Override
    public boolean isBegun() throws TransactionException {
      return false;
    }

    @Override
    public boolean isRolledBack() throws TransactionException {
      return false;
    }

    @Override
    public boolean isCommitted() throws TransactionException {
      return false;
    }

    @Override
    public int getTimeout() {
      return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
      this.timeout = timeout;
    }

    @Override
    public Object getResource(Object key) {
      return null;
    }

    @Override
    public boolean hasResource(Object key) {
      return false;
    }

    @Override
    public boolean supports(Object key, Object resource) {
      return true;
    }

    @Override
    public void bindResource(Object key, Object resource) throws TransactionException {}

    @Override
    public void setRollbackOnly() throws TransactionException {}

    @Override
    public boolean isRollbackOnly() throws TransactionException {
      return false;
    }

    @Override
    public boolean isXA() {
      return false;
    }

    @Override
    public void resume() throws TransactionException {}

    @Override
    public javax.transaction.Transaction suspend() throws TransactionException {
      return null;
    }

    @Override
    public String getId() {
      return null;
    }
  }
}
