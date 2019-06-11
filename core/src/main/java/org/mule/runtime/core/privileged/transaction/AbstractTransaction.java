/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction;

import static java.lang.System.identityHashCode;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.notification.TransactionNotification.TRANSACTION_BEGAN;
import static org.mule.runtime.api.notification.TransactionNotification.TRANSACTION_COMMITTED;
import static org.mule.runtime.api.notification.TransactionNotification.TRANSACTION_ROLLEDBACK;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.notMuleXaTransaction;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionMarkedForRollback;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;

import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * This base class provides low level features for transactions.
 */
public abstract class AbstractTransaction implements TransactionAdapter {

  private static final Logger LOGGER = getLogger(AbstractTransaction.class);

  protected String id = UUID.getUUID();

  protected int timeout;
  protected ComponentLocation componentLocation;

  protected MuleContext muleContext;
  private final NotificationDispatcher notificationFirer;

  protected AbstractTransaction(MuleContext muleContext) {
    this.muleContext = muleContext;
    try {
      notificationFirer = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean isRollbackOnly() throws TransactionException {
    int status = getStatus();
    return status == STATUS_MARKED_ROLLBACK || status == STATUS_ROLLEDBACK || status == STATUS_ROLLING_BACK;
  }

  @Override
  public boolean isBegun() throws TransactionException {
    int status = getStatus();
    return status != STATUS_NO_TRANSACTION && status != STATUS_UNKNOWN;
  }

  @Override
  public boolean isRolledBack() throws TransactionException {
    return getStatus() == STATUS_ROLLEDBACK;
  }

  @Override
  public boolean isCommitted() throws TransactionException {
    return getStatus() == STATUS_COMMITTED;
  }

  @Override
  public void begin() throws TransactionException {
    LOGGER.debug("Beginning transaction {}@{}", this.getClass().getName(), identityHashCode(this));
    doBegin();
    TransactionCoordination.getInstance().bindTransaction(this);
    fireNotification(new TransactionNotification(getId(), TRANSACTION_BEGAN, getApplicationName()));
  }

  @Override
  public void commit() throws TransactionException {
    try {
      LOGGER.debug("Committing transaction {}@{}", this.getClass().getName(), identityHashCode(this));

      if (isRollbackOnly()) {
        throw new IllegalTransactionStateException(transactionMarkedForRollback());
      }

      doCommit();
      fireNotification(new TransactionNotification(getId(), TRANSACTION_COMMITTED, getApplicationName()));
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(this);
    }
  }

  @Override
  public void rollback() throws TransactionException {
    try {
      LOGGER.debug("Rolling back transaction {}@{}", this.getClass().getName(), identityHashCode(this));
      setRollbackOnly();
      doRollback();
      fireNotification(new TransactionNotification(getId(), TRANSACTION_ROLLEDBACK, getApplicationName()));
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
   * {@link TransactionNotificationListener}s.
   *
   */
  protected void fireNotification(TransactionNotification notification) {
    // TODO profile this piece of code
    notificationFirer.dispatch(notification);
  }

  @Override
  public boolean isXA() {
    return false;
  }

  @Override
  public void resume() throws TransactionException {
    throw new IllegalTransactionStateException(notMuleXaTransaction(this));
  }

  @Override
  public javax.transaction.Transaction suspend() throws TransactionException {
    throw new IllegalTransactionStateException(notMuleXaTransaction(this));
  }

  @Override
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

  @Override
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  @Override
  public void setComponentLocation(ComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  @Override
  public Optional<ComponentLocation> getComponentLocation() {
    return ofNullable(componentLocation);
  }
}
