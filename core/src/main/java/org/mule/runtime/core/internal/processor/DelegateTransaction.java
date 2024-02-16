/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Optional.empty;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.privileged.transaction.AbstractTransaction;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

import java.util.Optional;

import javax.transaction.TransactionManager;

/**
 * Transaction placeholder to replace with proper transaction once transactional resource is discovered by mule
 */
public class DelegateTransaction extends AbstractTransaction {

  private static final Integer DEFAULT_TRANSACTION_TIMEOUT = 30000;

  private Transaction delegate = new NullTransaction();

  private SingleResourceTransactionFactoryManager transactionFactoryManager;
  private TransactionManager transactionManager;

  public DelegateTransaction(String applicationName, NotificationDispatcher notificationFirer,
                             SingleResourceTransactionFactoryManager transactionFactoryManager,
                             TransactionManager transactionManager) {
    super(applicationName, notificationFirer);
    this.transactionFactoryManager = transactionFactoryManager;
    this.transactionManager = transactionManager;
  }

  @Override
  protected void doBegin() {}

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
    TransactionFactory transactionFactory = transactionFactoryManager.getTransactionFactoryFor(key.getClass());
    this.unbindTransaction();
    this.delegate = transactionFactory.beginTransaction(applicationName, notificationFirer, transactionFactoryManager,
                                                        transactionManager);
    this.delegate.setTimeout(timeout);
    this.delegate.bindResource(key, resource);
    ((TransactionAdapter) delegate).setComponentLocation(componentLocation);
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

  private class NullTransaction implements TransactionAdapter {

    private Integer timeout;

    @Override
    public void begin() {}

    @Override
    public void commit() throws TransactionException {}

    @Override
    public void rollback() throws TransactionException {}

    @Override
    public int getStatus() {
      return Transaction.STATUS_UNKNOWN;
    }

    @Override
    public boolean isBegun() {
      return false;
    }

    @Override
    public boolean isRolledBack() {
      return false;
    }

    @Override
    public boolean isCommitted() {
      return false;
    }

    @Override
    public int getTimeout() {
      if (timeout != null) {
        return timeout;
      }
      timeout = DEFAULT_TRANSACTION_TIMEOUT;
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
    public void bindResource(Object key, Object resource) {}

    @Override
    public void setRollbackOnly() {}

    @Override
    public boolean isRollbackOnly() {
      return false;
    }

    @Override
    public boolean isXA() {
      return false;
    }

    @Override
    public void resume() {}

    @Override
    public javax.transaction.Transaction suspend() {
      return null;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public Optional<ComponentLocation> getComponentLocation() {
      return empty();
    }

    @Override
    public void setComponentLocation(ComponentLocation componentLocation) {

    }
  }
}
