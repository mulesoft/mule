/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * <p><code>XaTransaction</code> represents an XA transaction in Mule.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class XaTransaction implements UMOTransaction
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(XaTransaction.class);

    private Transaction transaction;

    private boolean started = false;
    private boolean rolledback = false;
    private boolean committed = false;
    private boolean rollbackOnly = false;

    /**
     * 
     */
    public XaTransaction(Transaction transaction)
    {
        this.transaction = transaction;

    }

    public void begin() throws UMOTransactionException
    {
        started = true;
    }

    public boolean isBegun()
    {
        return started;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#commit()
     */
    public void commit() throws UMOTransactionException
    {
        try
        {
            if (rollbackOnly)
            {
                throw new TransactionRollbackException("Transaction is marked for rollback only");
            }

            transaction.commit();
            committed = true;
        }
        catch (RollbackException e)
        {
            throw new TransactionRollbackException("transaction has already been marked for rollback", e);
        }
        catch (HeuristicRollbackException e)
        {
            throw new TransactionRollbackException("transaction has already been marked for rollback", e);
        }
        catch (Exception e)
        {
            throw new IllegalTransactionStateException("Transaction could not be committed: " + e, e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#isRolledBack()
     */
    public boolean isRolledBack() throws TransactionStatusException
    {
        return rolledback;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    public void rollback() throws TransactionRollbackException
    {
        try
        {
            transaction.rollback();
            rolledback = true;
        }
        catch (SystemException e)
        {
            throw new TransactionRollbackException("Failed to roll back: " + e.getMessage(), e);
        }

    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#isCommitted()
     */
    public boolean isCommitted() throws TransactionStatusException
    {
        return committed;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#getStatus()
     */
    public int getStatus() throws TransactionStatusException
    {
        try
        {
            return transaction.getStatus();
        }
        catch (SystemException e)
        {
            throw new TransactionStatusException("Unable to read transaction state: " + e.getMessage(), e);
        }
    }

    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException
    {
        transaction.registerSynchronization(synchronization);
    }

    public void setRollbackOnly() throws IllegalStateException
    {
//        try
//        {
            rollbackOnly = true;
            //If we set the state here we cannot continue the tx
            //transaction.setRollbackOnly();
//        }
//        catch (SystemException e)
//        {
//            throw new IllegalStateException("Failed to set transaction to rollback only: " + e.getMessage());
//        }
    }

    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException
    {
        return transaction.enlistResource(xaResource);
    }

    public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException
    {
        return transaction.delistResource(xaResource, i);
    }

    public Object getResource()
    {
        return transaction;
    }

    public boolean isRollbackOnly()
    {
        return rollbackOnly;
    }
}
