/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction;

import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.tx.MuleXaObject;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transaction.TransactionRollbackException;
import org.mule.runtime.core.api.transaction.TransactionStatusException;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.runtime.core.privileged.transaction.xa.XaResourceFactoryHolder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/**
 * <code>XaTransaction</code> represents an XA transaction in Mule.
 */
public class XaTransaction extends AbstractTransaction {

  /**
   * The inner JTA transaction
   */
  protected Transaction transaction = null;

  /**
   * Map of enlisted resources
   */
  private Map<ResourceKey, Object> resources = new HashMap<>();

  protected TransactionManager txManager;

  public XaTransaction(MuleContext context) {
    super(context);
    this.txManager = context.getTransactionManager();
  }

  protected void doBegin() throws TransactionException {
    if (txManager == null) {
      throw new IllegalStateException(CoreMessages
          .objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
    }

    try {
      txManager.setTransactionTimeout(getTimeoutInSeconds());
      txManager.begin();
      synchronized (this) {
        transaction = txManager.getTransaction();
      }
    } catch (Exception e) {
      throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
    }
  }

  protected synchronized void doCommit() throws TransactionException {
    try {
      /*
       * JTA spec quotes (parts highlighted by AP), the same applies to both TransactionManager and UserTransaction:
       * 
       * 3.2.2 Completing a Transaction The TransactionManager.commit method completes the transaction currently associated with
       * the calling thread.
       ****
       * 
       * After the commit method returns, the calling thread is not associated with a transaction.
       ****
       * 
       * If the commit method is called when the thread is not associated with any transaction context, the TM throws an
       * exception. In some implementations, the commit operation is restricted to the transaction originator only. If the calling
       * thread is not allowed to commit the transaction, the TM throws an exception. The TransactionManager.rollback method rolls
       * back the transaction associated with the current thread.
       ****
       * After the rollback method completes, the thread is associated with no transaction.
       ****
       * 
       * And the following block about Transaction (note there's no thread-tx disassociation clause)
       * 
       * 3.3.3 Transaction Completion The Transaction.commit and Transaction.rollback methods allow the target object to be
       * comitted or rolled back. The calling thread is not required to have the same transaction associated with the thread. If
       * the calling thread is not allowed to commit the transaction, the transaction manager throws an exception.
       * 
       * 
       * So what it meant was that one can't use Transaction.commit()/rollback(), as it doesn't properly disassociate the thread
       * of execution from the current transaction. There's no JTA API-way to do that after the call, so the thread's transaction
       * is subject to manual recovery process. Instead TransactionManager or UserTransaction must be used.
       */
      delistResources();
      txManager.commit();
    } catch (RollbackException | HeuristicRollbackException e) {
      throw new TransactionRollbackException(CoreMessages.transactionMarkedForRollback(), e);
    } catch (Exception e) {
      throw new IllegalTransactionStateException(CoreMessages.transactionCommitFailed(), e);
    } finally {
      /*
       * MUST nullify XA ref here, otherwise Transaction.getStatus() doesn't match javax.transaction.Transaction.getStatus(). Must
       * return STATUS_NO_TRANSACTION and not STATUS_COMMITTED.
       * 
       * TransactionCoordination unbinds the association immediately on this method's exit.
       */
      this.transaction = null;
      closeResources();
    }
  }

  protected void doRollback() throws TransactionRollbackException {
    try {
      /*
       * JTA spec quotes (parts highlighted by AP), the same applies to both TransactionManager and UserTransaction:
       * 
       * 3.2.2 Completing a Transaction The TransactionManager.commit method completes the transaction currently associated with
       * the calling thread.
       ****
       * 
       * After the commit method returns, the calling thread is not associated with a transaction.
       ****
       * 
       * If the commit method is called when the thread is not associated with any transaction context, the TM throws an
       * exception. In some implementations, the commit operation is restricted to the transaction originator only. If the calling
       * thread is not allowed to commit the transaction, the TM throws an exception. The TransactionManager.rollback method rolls
       * back the transaction associated with the current thread.
       ****
       * After the rollback method completes, the thread is associated with no transaction.
       ****
       * 
       * And the following block about Transaction (note there's no thread-tx disassociation clause)
       * 
       * 3.3.3 Transaction Completion The Transaction.commit and Transaction.rollback methods allow the target object to be
       * comitted or rolled back. The calling thread is not required to have the same transaction associated with the thread. If
       * the calling thread is not allowed to commit the transaction, the transaction manager throws an exception.
       * 
       * 
       * So what it meant was that one can't use Transaction.commit()/rollback(), as it doesn't properly disassociate the thread
       * of execution from the current transaction. There's no JTA API-way to do that after the call, so the thread's transaction
       * is subject to manual recovery process. Instead TransactionManager or UserTransaction must be used.
       */
      // delistResources();
      txManager.rollback();
    } catch (Exception e) {
      throw new TransactionRollbackException(e);
    } finally {
      /*
       * MUST nullify XA ref here, otherwise Transaction.getStatus() doesn't match javax.transaction.Transaction.getStatus(). Must
       * return STATUS_NO_TRANSACTION and not STATUS_COMMITTED.
       * 
       * TransactionCoordination unbinds the association immediately on this method's exit.
       */
      this.transaction = null;
      closeResources();
    }
  }

  public synchronized int getStatus() throws TransactionStatusException {
    if (transaction == null) {
      return STATUS_NO_TRANSACTION;
    }

    try {
      return transaction.getStatus();
    } catch (SystemException e) {
      throw new TransactionStatusException(e);
    }
  }

  public void setRollbackOnly() {
    if (transaction == null) {
      throw new IllegalStateException("Current thread is not associated with a transaction.");
    }

    try {
      synchronized (this) {
        transaction.setRollbackOnly();
      }
    } catch (SystemException e) {
      throw (IllegalStateException) new IllegalStateException("Failed to set transaction to rollback only: " + e.getMessage())
          .initCause(e);
    }
  }

  public synchronized Object getResource(Object key) {
    ResourceKey normalizedKey = getResourceEntry(key);
    return resources.get(normalizedKey);
  }

  public synchronized boolean hasResource(Object key) {
    ResourceKey normalizedKey = getResourceEntry(key);
    return resources.containsKey(normalizedKey);
  }

  /**
   * @param key Must be the provider of the resource object. i.e. for JDBC it's the XADataSource, for JMS is the
   *        XAConnectionFactory. It can be a wrapper in which case should be a
   *        {@link XaResourceFactoryHolder} to be able to determine correctly if there's already a
   *        resource for that {@link javax.transaction.xa.XAResource} provider.
   * @param resource the resource object. It must be an {@link javax.transaction.xa.XAResource} or a
   *        {@link MuleXaObject}
   * @throws TransactionException
   */
  public synchronized void bindResource(Object key, Object resource) throws TransactionException {
    ResourceKey normalizedKey = getResourceEntry(key, resource);
    if (resources.containsKey(key)) {
      throw new IllegalTransactionStateException(CoreMessages.transactionResourceAlreadyListedForKey(key));
    }

    resources.put(normalizedKey, resource);

    if (key == null) {
      logger.error("Key for bound resource " + resource + " is null");
    }

    if (resource instanceof MuleXaObject) {
      MuleXaObject xaObject = (MuleXaObject) resource;
      xaObject.enlist();
    } else if (resource instanceof XAResource) {
      enlistResource((XAResource) resource);
    } else {
      logger.error("Bound resource " + resource + " is neither a MuleXaObject nor XAResource");
    }
  }

  // moved here from connection wrapper
  public boolean enlistResource(XAResource resource) throws TransactionException {
    TransactionManager txManager = muleContext.getTransactionManager();
    try {
      Transaction jtaTransaction = txManager.getTransaction();
      if (jtaTransaction == null) {
        throw new TransactionException(I18nMessageFactory.createStaticMessage("XATransaction is null"));
      }
      resource.setTransactionTimeout(getTimeoutInSeconds());
      return jtaTransaction.enlistResource(resource);
    } catch (RollbackException | SystemException | XAException e) {
      throw new TransactionException(e);
    }
  }

  private int getTimeoutInSeconds() {
    // we need to convert milliseconds timeout to seconds timeout for XA
    return getTimeout() / 1000;
  }

  public boolean delistResource(XAResource resource, int tmflag) throws TransactionException {
    TransactionManager txManager = muleContext.getTransactionManager();
    try {
      Transaction jtaTransaction = txManager.getTransaction();
      if (jtaTransaction == null) {
        throw new TransactionException(CoreMessages.noJtaTransactionAvailable(Thread.currentThread()));
      }
      return jtaTransaction.delistResource(resource, tmflag);
    } catch (SystemException e) {
      throw new TransactionException(e);
    }
  }


  public String toString() {
    return transaction == null ? " <n/a>" : transaction.toString();
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public boolean isXA() {
    return true;
  }

  public void resume() throws TransactionException {
    TransactionManager txManager = muleContext.getTransactionManager();

    if (txManager == null) {
      throw new IllegalStateException(CoreMessages
          .objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
    }
    try {
      txManager.resume(transaction);
    } catch (InvalidTransactionException | SystemException e) {
      throw new TransactionException(e);
    }
  }

  public Transaction suspend() throws TransactionException {
    TransactionManager txManager = muleContext.getTransactionManager();

    if (txManager == null) {
      throw new IllegalStateException(CoreMessages
          .objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
    }
    try {
      transaction = txManager.suspend();
    } catch (SystemException e) {
      throw new TransactionException(e);
    }
    return transaction;
  }

  protected void delistResources() {
    for (Object o : resources.entrySet()) {
      Map.Entry entry = (Map.Entry) o;
      final Object xaObject = entry.getValue();
      if (xaObject instanceof MuleXaObject) {
        // there is need for reuse object
        try {
          ((MuleXaObject) xaObject).delist();
        } catch (Exception e) {
          logger.error("Failed to delist resource " + xaObject, e);
        }
      }
    }
  }

  protected void closeResources() {
    Iterator i = resources.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      final Object value = entry.getValue();
      if (value instanceof MuleXaObject) {
        MuleXaObject xaObject = (MuleXaObject) value;
        if (!xaObject.isReuseObject()) {
          try {
            xaObject.close();
            i.remove();
          } catch (Exception e) {
            logger.error("Failed to close resource " + xaObject, e);
          }
        }
      }
    }
  }

  @Override
  public boolean supports(Object key, Object resource) {
    return resource instanceof XAResource || resource instanceof MuleXaObject;
  }

  private ResourceKey getResourceEntry(Object resourceFactory) {
    resourceFactory = (resourceFactory instanceof XaResourceFactoryHolder
        ? ((XaResourceFactoryHolder) resourceFactory).getHoldObject() : resourceFactory);
    return new ResourceKey(resourceFactory, null);
  }

  private ResourceKey getResourceEntry(Object resourceFactory, Object resource) {
    resourceFactory = (resourceFactory instanceof XaResourceFactoryHolder
        ? ((XaResourceFactoryHolder) resourceFactory).getHoldObject() : resourceFactory);
    return new ResourceKey(resourceFactory, resource);
  }

  /**
   * This class is used as key for the resources map since allows us to overcome some bad hashcode implementation of resource
   * factories such as org.enhydra.jdbc.standard.StandardDataSource.
   */
  private static class ResourceKey {

    private Object resourceFactory;
    private Object resource;

    public ResourceKey(Object resourceFactory) {
      Preconditions.checkArgument(resourceFactory != null, "resourceFactory cannot be null");
      this.resourceFactory = resourceFactory;
      this.resource = null;
    }

    public ResourceKey(Object resourceFactory, Object resource) {
      this(resourceFactory);
      this.resource = resource;
    }

    public Object getResourceFactory() {
      return resourceFactory;
    }

    public Object getResource() {
      return resource;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(resourceFactory);
    }

    @Override
    public boolean equals(Object obj) {
      // we use this class internally only so are sure obj is always a ResourceEntry
      return resourceFactory.equals(((ResourceKey) obj).getResourceFactory());
    }
  }
}
