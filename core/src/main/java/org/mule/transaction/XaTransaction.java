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
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.umo.TransactionException;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.HeuristicRollbackException;
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
    private Map resources = null;

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
                CoreMessages.objectNotRegistered("Transaction Manager", "Transaction Manager").getMessage());
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

    protected void doCommit() throws TransactionException
    {
        try
        {
            synchronized (this)
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
                // TODO AP check this way to get to TM really works in Mule 2
                TransactionManager txManager = MuleServer.getManagementContext().getTransactionManager();
                txManager.commit();
            }
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
                MUST nullify XA ref here, otherwise UMOTransaction.getStatus() doesn't match
                javax.transaction.Transaction.getStatus(). Must return STATUS_NO_TRANSACTION and not
                STATUS_COMMITTED.

                TransactionCoordination unbinds the association immediately on this method's exit.
            */
            this.transaction = null;
        }
    }

    protected void doRollback() throws TransactionRollbackException
    {
        try
        {
            synchronized (this)
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
                TransactionManager txManager = MuleServer.getManagementContext().getTransactionManager();
                txManager.rollback();
            }
        }
        catch (SystemException e)
        {
            throw new TransactionRollbackException(e);
        }
        finally
        {
            /*
                MUST nullify XA ref here, otherwise UMOTransaction.getStatus() doesn't match
                javax.transaction.Transaction.getStatus(). Must return STATUS_NO_TRANSACTION and not
                STATUS_COMMITTED.

                TransactionCoordination unbinds the association immediately on this method's exit.
            */
            this.transaction = null;
        }
    }

    public int getStatus() throws TransactionStatusException
    {
        synchronized (this)
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

    public Object getResource(Object key)
    {
        synchronized (this)
        {
            return resources == null ? null : resources.get(key);
        }
    }

    public boolean hasResource(Object key)
    {
        synchronized (this)
        {
            return resources != null && resources.containsKey(key);
        }
    }

    public void bindResource(Object key, Object resource) throws TransactionException
    {
        synchronized (this)
        {
            if (resources == null)
            {
                resources = new HashMap();
            }

            if (resources.containsKey(key))
            {
                throw new IllegalTransactionStateException(
                        CoreMessages.transactionResourceAlreadyListedForKey(key));
            }

            resources.put(key, resource);
        }
    }

    // moved here from connection wrapper
    public boolean enlistResource(XAResource resource) throws TransactionException
    {
        TransactionManager txManager = MuleServer.getManagementContext().getTransactionManager();
        try
        {
            Transaction tx = txManager.getTransaction();
            if (tx == null)
            {
                throw new TransactionException(MessageFactory.createStaticMessage("XATransaction is null"));
            }
            return tx.enlistResource(resource);
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

    public String toString()
    {
        return transaction == null ? " <null>" : transaction.toString();
    }
}
