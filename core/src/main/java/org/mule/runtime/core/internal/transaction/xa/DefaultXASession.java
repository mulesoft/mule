/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction.xa;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for an XAResource implementation.
 *
 * @param <T> type of the {@link AbstractXaTransactionContext} created for each transaction
 */
public abstract class DefaultXASession<T extends AbstractXaTransactionContext> implements XAResource {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());
  private Xid localXid;
  private AbstractXAResourceManager<T> resourceManager;
  private T localContext;

  public DefaultXASession(AbstractXAResourceManager resourceManager) {
    this.localContext = null;
    this.localXid = null;
    this.resourceManager = resourceManager;
  }

  public XAResource getXAResource() {
    return this;
  }

  public Object getResourceManager() {
    return resourceManager;
  }

  //
  // XAResource implementation
  //

  public boolean isSameRM(XAResource xares) throws XAException {
    return xares instanceof DefaultXASession && ((DefaultXASession) xares).getResourceManager().equals(resourceManager);
  }

  public void start(Xid xid, int flags) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder(128).append("Thread ").append(Thread.currentThread())
          .append(flags == TMNOFLAGS ? " starts" : flags == TMJOIN ? " joins" : " resumes")
          .append(" work on behalf of transaction branch ").append(xid).toString());
    }
    // A local transaction is already begun
    if (this.localContext != null) {
      throw new XAException(XAException.XAER_PROTO);
    }
    // This session has already been associated with an xid
    if (this.localXid != null) {
      throw new XAException(XAException.XAER_PROTO);
    }
    switch (flags) {
      // a new transaction
      case TMNOFLAGS:
      case TMJOIN:
      default:
        try {
          localContext = createTransactionContext(xid);
          resourceManager.beginTransaction(localContext);
        } catch (Exception e) {
          // TODO MULE-863: Is logging necessary?
          logger.error("Could not create new transactional resource", e);
          throw (XAException) new XAException(e.getMessage()).initCause(e);
        }
        break;
      case TMRESUME:
        localContext = resourceManager.getSuspendedTransactionalResource(xid);
        if (localContext == null) {
          throw new XAException(XAException.XAER_NOTA);
        }
        // TODO: resume context
        resourceManager.removeSuspendedTransactionalResource(xid);
        break;
    }
    localXid = xid;
    resourceManager.addActiveTransactionalResource(localXid, localContext);
  }

  public void end(Xid xid, int flags) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder(128).append("Thread ").append(Thread.currentThread())
          .append(flags == TMSUSPEND ? " suspends" : flags == TMFAIL ? " fails" : " ends")
          .append(" work on behalf of transaction branch ").append(xid).toString());
    }
    // No transaction is already begun
    if (localContext == null) {
      throw new XAException(XAException.XAER_NOTA);
    }
    // This session has already been associated with an xid
    if (localXid == null || !localXid.equals(xid)) {
      throw new XAException(XAException.XAER_PROTO);
    }

    try {
      switch (flags) {
        case TMSUSPEND:
          // TODO: suspend context
          resourceManager.addSuspendedTransactionalResource(localXid, localContext);
          resourceManager.removeActiveTransactionalResource(localXid);
          break;
        case TMFAIL:
          resourceManager.setTransactionRollbackOnly(localContext);
          break;
        case TMSUCCESS: // no-op
        default: // no-op
          break;
      }
    } catch (ResourceManagerException e) {
      throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
    }
    localXid = null;
    localContext = null;
  }

  public void commit(Xid xid, boolean onePhase) throws XAException {
    if (xid == null) {
      throw new XAException(XAException.XAER_PROTO);
    }
    T context = resourceManager.getActiveTransactionalResource(xid);
    if (context == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Commit called without a transaction context");
      }
      commitDanglingTransaction(xid, onePhase);
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Committing transaction branch " + xid);
    }
    if (context.status == Status.STATUS_MARKED_ROLLBACK) {
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    try {
      if (context.status != Status.STATUS_PREPARED) {
        if (onePhase) {
          resourceManager.prepareTransaction(context);
        } else {
          throw new XAException(XAException.XAER_PROTO);
        }
      }
      resourceManager.commitTransaction(context);
      localContext = null;
    } catch (ResourceManagerException e) {
      throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
    }
    resourceManager.removeActiveTransactionalResource(xid);
    resourceManager.removeSuspendedTransactionalResource(xid);
  }

  public void rollback(Xid xid) throws XAException {
    if (xid == null) {
      throw new XAException(XAException.XAER_PROTO);
    }
    AbstractTransactionContext context = resourceManager.getActiveTransactionalResource(xid);
    if (context == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Rollback called without a transaction context");
      }
      rollbackDandlingTransaction(xid);
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Rolling back transaction branch " + xid);
    }
    try {
      resourceManager.rollbackTransaction(context);
      localContext = null;
    } catch (ResourceManagerException e) {
      throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
    }
    resourceManager.removeActiveTransactionalResource(xid);
    resourceManager.removeSuspendedTransactionalResource(xid);
  }

  public int prepare(Xid xid) throws XAException {
    if (xid == null) {
      throw new XAException(XAException.XAER_PROTO);
    }

    T context = resourceManager.getTransactionalResource(xid);
    if (context == null) {
      throw new XAException(XAException.XAER_NOTA);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Preparing transaction branch " + xid);
    }

    if (context.status == Status.STATUS_MARKED_ROLLBACK) {
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    try {
      return resourceManager.prepareTransaction(context);
    } catch (ResourceManagerException e) {
      throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
    }
  }

  public void forget(Xid xid) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Forgetting transaction branch " + xid);
    }
    AbstractTransactionContext context = resourceManager.getTransactionalResource(xid);
    if (context == null) {
      throw new XAException(XAException.XAER_NOTA);
    }
    resourceManager.removeActiveTransactionalResource(xid);
    resourceManager.removeSuspendedTransactionalResource(xid);
  }

  public int getTransactionTimeout() throws XAException {
    return (int) (resourceManager.getDefaultTransactionTimeout() / 1000);
  }

  public boolean setTransactionTimeout(int timeout) throws XAException {
    resourceManager.setDefaultTransactionTimeout(timeout * 1000);
    return false;
  }

  public T getTransactionContext() {
    return this.localContext;
  }

  /**
   * Commits a dangling transaction that can be caused by the failure of one of the XAResource involved in the transaction or a
   * crash of the transaction manager.
   *
   * @param xid transaction identifier
   * @param onePhase if the commit should be done using only one phase commit
   * @throws XAException
   */
  protected abstract void commitDanglingTransaction(Xid xid, boolean onePhase) throws XAException;

  /**
   * Commits a dangling transaction that can be caused by the failure of one of the XAResource involved in the transaction or a
   * crash of the transaction manager.
   *
   * @param xid transaction identifier
   * @throws XAException
   */
  protected abstract void rollbackDandlingTransaction(Xid xid) throws XAException;

  /**
   * Creates a new transaction context with the given transaction identifier
   *
   * @param xid transaction identifier
   * @return the new transaction context
   */
  abstract protected T createTransactionContext(Xid xid);

}
