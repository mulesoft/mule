/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transaction;

import static java.lang.System.identityHashCode;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.api.util.UUID;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This base class provides low level features for transactions.
 */
public abstract class AbstractTransaction implements Transaction {

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  protected String id = UUID.getUUID();

  protected int timeout;

  protected MuleContext muleContext;

  protected AbstractTransaction(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public boolean isRollbackOnly() throws TransactionException {
    int status = getStatus();
    return status == STATUS_MARKED_ROLLBACK || status == STATUS_ROLLEDBACK || status == STATUS_ROLLING_BACK;
  }

  public boolean isBegun() throws TransactionException {
    int status = getStatus();
    return status != STATUS_NO_TRANSACTION && status != STATUS_UNKNOWN;
  }

  public boolean isRolledBack() throws TransactionException {
    return getStatus() == STATUS_ROLLEDBACK;
  }

  public boolean isCommitted() throws TransactionException {
    return getStatus() == STATUS_COMMITTED;
  }

  public void begin() throws TransactionException {
    logger.debug("Beginning transaction " + identityHashCode(this));
    doBegin();
    TransactionCoordination.getInstance().bindTransaction(this);
    fireNotification(new TransactionNotification(this, TransactionNotification.TRANSACTION_BEGAN, getApplicationName()));
  }

  public void commit() throws TransactionException {
    try {
      logger.debug("Committing transaction " + identityHashCode(this));

      if (isRollbackOnly()) {
        throw new IllegalTransactionStateException(CoreMessages.transactionMarkedForRollback());
      }

      doCommit();
      fireNotification(new TransactionNotification(this, TransactionNotification.TRANSACTION_COMMITTED, getApplicationName()));
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(this);
    }
  }

  public void rollback() throws TransactionException {
    try {
      logger.debug("Rolling back transaction " + identityHashCode(this));
      setRollbackOnly();
      doRollback();
      fireNotification(new TransactionNotification(this, TransactionNotification.TRANSACTION_ROLLEDBACK, getApplicationName()));
    } finally {
      unbindTransaction();
    }
  }

  /**
   * Unbind this transaction when complete
   */
  protected void unbindTransaction() throws TransactionException {
    TransactionCoordination.getInstance().unbindTransaction(this);
  }


  /**
   * Really begin the transaction. Note that resources are enlisted yet.
   * 
   * @throws TransactionException
   */
  protected abstract void doBegin() throws TransactionException;

  /**
   * Commit the transaction on the underlying resource
   * 
   * @throws TransactionException
   */
  protected abstract void doCommit() throws TransactionException;

  /**
   * Rollback the transaction on the underlying resource
   * 
   * @throws TransactionException
   */
  protected abstract void doRollback() throws TransactionException;

  /**
   * Fires a server notification to all registered
   * {@link org.mule.runtime.core.api.context.notification.TransactionNotificationListener}s.
   *
   */
  protected void fireNotification(TransactionNotification notification) {
    // TODO profile this piece of code
    muleContext.fireNotification(notification);
  }

  public boolean isXA() {
    return false;
  }

  public void resume() throws TransactionException {
    throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(this));
  }

  public javax.transaction.Transaction suspend() throws TransactionException {
    throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(this));
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    int status;
    try {
      status = getStatus();
    } catch (TransactionException e) {
      status = -1;
    }
    return MessageFormat.format("{0}[id={1} , status={2}]", getClass().getName(), id, status);
  }

  private String getApplicationName() {
    return muleContext.getConfiguration().getId();
  }

  @Override
  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
