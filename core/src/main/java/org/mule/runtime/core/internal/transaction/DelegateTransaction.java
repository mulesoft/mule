/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

import java.util.Optional;

/**
 * Transaction placeholder to replace with proper transaction once transactional resource is discovered by mule
 */
public class DelegateTransaction extends AbstractTransaction {

  private static final Integer DEFAULT_TRANSACTION_TIMEOUT = 30000;

  private static final Optional<TransactionFactory> TX_FACTORY;

  static {
    TX_FACTORY = stream(load(TransactionFactory.class,
                             DelegateTransaction.class.getClassLoader()).spliterator(),
                        false)
                            .findFirst();
  }

  private SuspendableTransaction delegate = new NullTransaction();

  public DelegateTransaction(String applicationName, NotificationDispatcher notificationFirer) {
    super(applicationName, notificationFirer);
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
      throw new TransactionException(createStaticMessage("Single resource transaction has already a resource bound"));
    }

    this.unbindTransaction();
    this.delegate = (SuspendableTransaction) TX_FACTORY
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("No %s for transactional resource %s",
                                                                               TransactionFactory.class.getName(),
                                                                               key.getClass().getName()))))
        .beginTransaction(applicationName,
                          notificationFirer);
    this.delegate.setTimeout(timeout);
    ((TransactionAdapter) delegate).setRollbackIfTimeout(this.rollbackAfterTimeout);
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
  public jakarta.transaction.Transaction suspend() throws TransactionException {
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
    super.setTimeout(timeout);
    delegate.setTimeout(timeout);
  }

  @Override
  public void setRollbackIfTimeout(boolean rollbackAfterTimeout) {
    super.setRollbackIfTimeout(rollbackAfterTimeout);
    ((TransactionAdapter) delegate).setRollbackIfTimeout(rollbackAfterTimeout);
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
    public jakarta.transaction.Transaction suspend() {
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

    @Override
    public void setRollbackIfTimeout(boolean errorIfTimeout) {

    }
  }
}
