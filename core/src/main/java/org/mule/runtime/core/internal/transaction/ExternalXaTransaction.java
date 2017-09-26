/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.privileged.transaction.AbstractTransaction;
import org.mule.runtime.core.privileged.transaction.XaTransaction;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.text.MessageFormat;

import javax.transaction.Status;
import javax.transaction.Synchronization;

/**
 * <code>ExternalXaTransaction</code> represents an external XA transaction in Mule.
 */
public class ExternalXaTransaction extends XaTransaction {

  public ExternalXaTransaction(MuleContext muleContext) {
    super(muleContext);
  }

  protected void doBegin() throws TransactionException {
    if (txManager == null) {
      throw new IllegalStateException(CoreMessages
          .objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
    }

    try {
      synchronized (this) {
        transaction = txManager.getTransaction();
        transaction.registerSynchronization(new ExternalTransaction(muleContext));
      }
    } catch (Exception e) {
      throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
    }
  }

  /**
   * This class is notified when an external transaction is complete and cleans up Mule-specific resources
   */
  class ExternalTransaction extends AbstractTransaction implements Synchronization {

    ExternalTransaction(MuleContext muleContext) {
      super(muleContext);
    }

    /** Nothing to do */
    public void beforeCompletion() {}

    /** Clean up mule resources */
    public void afterCompletion(int status) {
      boolean commit = status == Status.STATUS_COMMITTED;

      try {
        if (commit) {
          commit();
        } else {
          rollback();
        }
      } catch (TransactionException ex) {
        logger.warn(MessageFormat.format("Exception while {0} an external transaction {1}",
                                         commit ? "committing" : "rolling back", this),
                    ex);
      }
    }

    @Override
    protected void unbindTransaction() {
      // no-op -- already unbound in TransactionTemplate
    }

    @Override
    protected void doCommit() {
      delistResources();
      closeResources();
      transaction = null;
    }

    @Override
    protected void doRollback() {
      closeResources();
      transaction = null;
    }

    @Override
    protected void doBegin() {}

    @Override
    public boolean isRollbackOnly() throws TransactionException {
      return ExternalXaTransaction.this.isRollbackOnly();
    }

    public int getStatus() throws TransactionException {
      return ExternalXaTransaction.this.getStatus();
    }

    public Object getResource(Object key) {
      return ExternalXaTransaction.this.getResource(key);
    }

    public boolean hasResource(Object key) {
      return ExternalXaTransaction.this.hasResource(key);
    }

    @Override
    public boolean supports(Object key, Object resource) {
      return ExternalXaTransaction.this.supports(key, resource);
    }

    public void bindResource(Object key, Object resource) throws TransactionException {}

    public void setRollbackOnly() throws TransactionException {}
  }
}
