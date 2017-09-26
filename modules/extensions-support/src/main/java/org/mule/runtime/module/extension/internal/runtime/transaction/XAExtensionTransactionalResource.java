/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static javax.transaction.xa.XAResource.TMSUCCESS;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.runtime.api.tx.MuleXaObject;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.transaction.XaTransaction;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;

import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialization of {@link ExtensionTransactionalResource} capable of joining XA transactions.
 *
 * @param <T> the generic type of the {@link XATransactionalConnection} that will join the transaction
 * @since 4.0
 */
public class XAExtensionTransactionalResource<T extends XATransactionalConnection> extends ExtensionTransactionalResource<T>
    implements MuleXaObject {

  private static final Logger LOGGER = LoggerFactory.getLogger(XAExtensionTransactionalResource.class);

  private XAResource enlistedXAResource;

  private volatile boolean reuseObject = false;

  /**
   * {@inheritDoc}
   */
  public XAExtensionTransactionalResource(T connection, ConnectionHandler<T> connectionHandler, Transaction transaction) {
    super(connection, connectionHandler, transaction);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean enlist() throws TransactionException {
    XaTransaction transaction = getTransaction();

    synchronized (this) {
      if (!isEnlisted()) {
        final XAResource xaResource = getConnection().getXAResource();
        boolean wasAbleToEnlist = transaction.enlistResource(xaResource);
        if (wasAbleToEnlist) {
          enlistedXAResource = xaResource;
        }
      }
    }

    return isEnlisted();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean delist() throws Exception {
    if (!isEnlisted()) {
      return false;
    }

    XaTransaction transaction = getTransaction();

    synchronized (this) {
      if (isEnlisted()) {
        boolean wasAbleToDelist = transaction.delistResource(enlistedXAResource, TMSUCCESS);
        if (wasAbleToDelist) {
          enlistedXAResource = null;
        }
      }
      return !isEnlisted();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws Exception {
    try {
      getConnection().close();
    } catch (Exception e) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Exception while explicitly closing the xaConnection (some providers require this). "
            + "The exception will be ignored and only logged: " + e.getMessage(), e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReuseObject() {
    return reuseObject;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setReuseObject(boolean reuseObject) {
    this.reuseObject = reuseObject;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getTargetObject() {
    return getConnection();
  }

  private boolean isEnlisted() {
    synchronized (this) {
      return enlistedXAResource != null;
    }
  }

  private XaTransaction getTransaction() throws TransactionException {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction == null) {
      throw new IllegalTransactionStateException(CoreMessages.noMuleTransactionAvailable());
    }
    if (!(transaction instanceof XaTransaction)) {
      throw new IllegalTransactionStateException(CoreMessages.notMuleXaTransaction(transaction));
    }

    return (XaTransaction) transaction;
  }

}
