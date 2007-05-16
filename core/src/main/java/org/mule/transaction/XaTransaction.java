/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.TransactionException;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
                transaction.commit();
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
    }

    protected void doRollback() throws TransactionRollbackException
    {
        try
        {
            synchronized (this)
            {
                transaction.rollback();
            }
        }
        catch (SystemException e)
        {
            throw new TransactionRollbackException(e);
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
}
