/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.MuleServer;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

/**
 * <code>XaTransaction</code> represents an XA transaction in Mule.
 */
public class XaTransaction extends AbstractTransaction
{
    /**
     * The inner JTA transaction
     */
    private Transaction transaction = null;

    /**
     * Map of enlisted resources
     */
    private Map resources = new HashMap();

    private TransactionManager txManager;

    public XaTransaction(TransactionManager txManager)
    {
        this.txManager = txManager;
    }
    
    protected void doBegin() throws TransactionException
    {
        if (txManager == null)
        {
            throw new IllegalStateException(
                    CoreMessages.objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
        }

        try
        {
            txManager.begin();
            synchronized (this)
            {
                transaction = txManager.getTransaction();
            }
        }
        catch (Exception e)
        {
            throw new TransactionException(CoreMessages.cannotStartTransaction("XA"), e);
        }
    }

    protected synchronized void doCommit() throws TransactionException
    {
        try
        {
            /*
           JTA spec quotes (parts highlighted by AP), the same applies to both TransactionManager and UserTransaction:

           3.2.2 Completing a Transaction
           The TransactionManager.commit method completes the transaction currently
           associated with the calling thread.

           ****
           After the commit method returns, the calling thread is not associated with a transaction.
           ****

           If the commit method is called when the thread is
           not associated with any transaction context, the TM throws an exception. In some
           implementations, the commit operation is restricted to the transaction originator only.
           If the calling thread is not allowed to commit the transaction, the TM throws an
           exception.
           The TransactionManager.rollback method rolls back the transaction associated
           with the current thread.
           ****
           After the rollback method completes, the thread is associated with no transaction.
           ****

           And the following block about Transaction (note there's no thread-tx disassociation clause)

           3.3.3 Transaction Completion
           The Transaction.commit and Transaction.rollback methods allow the target
           object to be comitted or rolled back. The calling thread is not required to have the same
           transaction associated with the thread.
           If the calling thread is not allowed to commit the transaction, the transaction manager
           throws an exception.


           So what it meant was that one can't use Transaction.commit()/rollback(), as it doesn't
           properly disassociate the thread of execution from the current transaction. There's no
           JTA API-way to do that after the call, so the thread's transaction is subject to manual
           recovery process. Instead TransactionManager or UserTransaction must be used.
            */
            delistResources();
            txManager.commit();
        }
        catch (RollbackException e)
        {
            throw new TransactionRollbackException(CoreMessages.transactionMarkedForRollback(), e);
        }
        catch (HeuristicRollbackException e)
        {
            throw new TransactionRollbackException(CoreMessages.transactionMarkedForRollback(), e);
        }
        catch (Exception e)
        {
            throw new IllegalTransactionStateException(CoreMessages.transactionCommitFailed(), e);
        }
        finally
        {
            /*
                MUST nullify XA ref here, otherwise Transaction.getStatus() doesn't match
                javax.transaction.Transaction.getStatus(). Must return STATUS_NO_TRANSACTION and not
                STATUS_COMMITTED.

                TransactionCoordination unbinds the association immediately on this method's exit.
            */
            this.transaction = null;
            closeResources();
        }
    }

    protected void doRollback() throws TransactionRollbackException
    {
        try
        {
            /*
           JTA spec quotes (parts highlighted by AP), the same applies to both TransactionManager and UserTransaction:

           3.2.2 Completing a Transaction
           The TransactionManager.commit method completes the transaction currently
           associated with the calling thread.

           ****
           After the commit method returns, the calling thread is not associated with a transaction.
           ****

           If the commit method is called when the thread is
           not associated with any transaction context, the TM throws an exception. In some
           implementations, the commit operation is restricted to the transaction originator only.
           If the calling thread is not allowed to commit the transaction, the TM throws an
           exception.
           The TransactionManager.rollback method rolls back the transaction associated
           with the current thread.
           ****
           After the rollback method completes, the thread is associated with no transaction.
           ****

           And the following block about Transaction (note there's no thread-tx disassociation clause)

           3.3.3 Transaction Completion
           The Transaction.commit and Transaction.rollback methods allow the target
           object to be comitted or rolled back. The calling thread is not required to have the same
           transaction associated with the thread.
           If the calling thread is not allowed to commit the transaction, the transaction manager
           throws an exception.


           So what it meant was that one can't use Transaction.commit()/rollback(), as it doesn't
           properly disassociate the thread of execution from the current transaction. There's no
           JTA API-way to do that after the call, so the thread's transaction is subject to manual
           recovery process. Instead TransactionManager or UserTransaction must be used.
            */
            //delistResources();
            txManager.rollback();
        }
        catch (SystemException e)
        {
            throw new TransactionRollbackException(e);
        }
        catch (Exception e)
        {
            throw new TransactionRollbackException(e);
        }
        finally
        {
            /*
                MUST nullify XA ref here, otherwise Transaction.getStatus() doesn't match
                javax.transaction.Transaction.getStatus(). Must return STATUS_NO_TRANSACTION and not
                STATUS_COMMITTED.

                TransactionCoordination unbinds the association immediately on this method's exit.
            */
            this.transaction = null;
            closeResources();
        }
    }

    public synchronized int getStatus() throws TransactionStatusException
    {
        if (transaction == null)
        {
            return STATUS_NO_TRANSACTION;
        }

        try
        {
            return transaction.getStatus();
        }
        catch (SystemException e)
        {
            throw new TransactionStatusException(e);
        }
    }

    public void setRollbackOnly()
    {
        if (transaction == null)
        {
            throw new IllegalStateException("Current thread is not associated with a transaction.");
        }

        try
        {
            synchronized (this)
            {
                transaction.setRollbackOnly();
            }
        }
        catch (SystemException e)
        {
            throw (IllegalStateException) new IllegalStateException(
                    "Failed to set transaction to rollback only: " + e.getMessage()
            ).initCause(e);
        }
    }

    public synchronized Object getResource(Object key)
    {
        return resources.get(key);
    }

    public synchronized boolean hasResource(Object key)
    {
        return resources.containsKey(key);
    }

    public synchronized void bindResource(Object key, Object resource) throws TransactionException
    {
        if (resources.containsKey(key))
        {
            throw new IllegalTransactionStateException(
                    CoreMessages.transactionResourceAlreadyListedForKey(key));
        }

        resources.put(key, resource);
        
        if (key == null)
        {
            logger.error("Key for bound resource " + resource + " is null");
        }
        
        if (resource instanceof MuleXaObject)
        {
            MuleXaObject xaObject = (MuleXaObject) resource;
            xaObject.enlist();
        }
        else if (resource instanceof XAResource)
        {
            enlistResource((XAResource) resource);
        }
        else
        {
            logger.error("Bound resource " + resource + " is neither a MuleXaObject nor XAResource");
        }
    }


    // moved here from connection wrapper
    public boolean enlistResource(XAResource resource) throws TransactionException
    {
        TransactionManager txManager = MuleServer.getMuleContext().getTransactionManager();
        try
        {
            Transaction jtaTransaction = txManager.getTransaction();
            if (jtaTransaction == null)
            {
                throw new TransactionException(MessageFactory.createStaticMessage("XATransaction is null"));
            }
            return jtaTransaction.enlistResource(resource);
        }
        catch (RollbackException e)
        {
            throw new TransactionException(e);
        }
        catch (SystemException e)
        {
            throw new TransactionException(e);
        }
    }

    public boolean delistResource(XAResource resource, int tmflag) throws TransactionException
    {
        TransactionManager txManager = MuleServer.getMuleContext().getTransactionManager();
        try
        {
            Transaction jtaTransaction = txManager.getTransaction();
            if (jtaTransaction == null)
            {
                throw new TransactionException(CoreMessages.noJtaTransactionAvailable(Thread.currentThread()));
            }
            return jtaTransaction.delistResource(resource, tmflag);
        }
        catch (SystemException e)
        {
            throw new TransactionException(e);
        }
    }


    public String toString()
    {
        return transaction == null ? " <n/a>" : transaction.toString();
    }

    public Transaction getTransaction()
    {
        return transaction;
    }

    public boolean isXA()
    {
        return true;
    }

    public void resume() throws TransactionException
    {
        TransactionManager txManager = MuleServer.getMuleContext().getTransactionManager();

        if (txManager == null)
        {
            throw new IllegalStateException(
                    CoreMessages.objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
        }
        try
        {
            txManager.resume(transaction);
        }
        catch (InvalidTransactionException e)
        {
            throw new TransactionException(e);
        }
        catch (SystemException e)
        {
            throw new TransactionException(e);
        }
    }

    public Transaction suspend() throws TransactionException
    {
        TransactionManager txManager = MuleServer.getMuleContext().getTransactionManager();

        if (txManager == null)
        {
            throw new IllegalStateException(
                    CoreMessages.objectNotRegistered("javax.transaction.TransactionManager", "Transaction Manager").getMessage());
        }
        try
        {
            transaction = txManager.suspend();
        }
        catch (SystemException e)
        {
            throw new TransactionException(e);
        }
        return transaction;
    }

    protected void delistResources()
    {
        Iterator i = resources.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry) i.next();
            final Object xaObject = entry.getValue();
            if (xaObject instanceof MuleXaObject)
            {
                //there is need for reuse object
                try
                {
                    ((MuleXaObject) xaObject).delist();
                }
                catch (Exception e)
                {
                    logger.error("Failed to delist resource " + xaObject, e);
                }
            }
        }
    }

    protected void closeResources()
    {
        Iterator i = resources.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry) i.next();
            final Object value = entry.getValue();
            if (value instanceof MuleXaObject)
            {
                MuleXaObject xaObject = (MuleXaObject) value;
                if (!xaObject.isReuseObject())
                {
                    try
                    {
                        xaObject.close();
                        i.remove();
                    }
                    catch (Exception e)
                    {
                        logger.error("Failed to close resource " + xaObject, e);
                    }
                }
            }
        }
    }

    public static interface MuleXaObject
    {

        void close() throws Exception;

        void setReuseObject(boolean reuseObject);

        boolean isReuseObject();

        boolean enlist() throws TransactionException;
        
        boolean delist() throws Exception;

        /**
         * Get XAConnection or XASession from wrapper / proxy
         *
         * @return return javax.sql.XAConnection for jdbc or javax.jms.XASession for jms
         */
        Object getTargetObject();

        String SET_REUSE_OBJECT_METHOD_NAME = "setReuseObject";
        String IS_REUSE_OBJECT_METHOD_NAME = "isReuseObject";
        String DELIST_METHOD_NAME = "delist";
        String ENLIST_METHOD_NAME = "enlist";
        String GET_TARGET_OBJECT_METHOD_NAME = "getTargetObject";
        String CLOSE_METHOD_NAME = "close";
    }

}
