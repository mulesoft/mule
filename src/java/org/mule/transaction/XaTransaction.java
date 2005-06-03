/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.transaction;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;

/**
 * <p>
 * <code>XaTransaction</code> represents an XA transaction in Mule.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class XaTransaction extends AbstractTransaction implements UMOTransaction
{
    /**
     * The inner jta transaction
     */
    private Transaction transaction = null;

    /**
     * Map of enlisted resources
     */
    private Map resources = null;

    /**
     * Default constructor
     */
    public XaTransaction()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#begin()
     */
    protected void doBegin() throws TransactionException
    {
        TransactionManager txManager = MuleManager.getInstance().getTransactionManager();
        if (txManager == null) {
            throw new IllegalStateException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Transaction Manager").getMessage());
        }
        try {
            txManager.begin();
            transaction = txManager.getTransaction();
        } catch (Exception e) {
            throw new TransactionException(new Message(Messages.TX_CANT_START_X_TRANSACTION, "XA"), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#commit()
     */
    protected void doCommit() throws TransactionException
    {
        try {
            transaction.commit();
        } catch (RollbackException e) {
            throw new TransactionRollbackException(new Message(Messages.TX_MARKED_FOR_ROLLBACK), e);
        } catch (HeuristicRollbackException e) {
            throw new TransactionRollbackException(new Message(Messages.TX_MARKED_FOR_ROLLBACK), e);
        } catch (Exception e) {
            throw new IllegalTransactionStateException(new Message(Messages.TX_COMMIT_FAILED), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    protected void doRollback() throws TransactionRollbackException
    {
        try {
            transaction.rollback();
        } catch (SystemException e) {
            throw new TransactionRollbackException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#getStatus()
     */
    public int getStatus() throws TransactionStatusException
    {
        if (transaction == null) {
            return STATUS_NO_TRANSACTION;
        }
        try {
            return transaction.getStatus();
        } catch (SystemException e) {
            throw new TransactionStatusException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#setRollbackOnly()
     */
    public void setRollbackOnly()
    {
        try {
            transaction.setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException("Failed to set transaction to rollback only: " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#getResource(java.lang.Object)
     */
    public Object getResource(Object key)
    {
        return resources == null ? null : resources.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#hasResource(java.lang.Object)
     */
    public boolean hasResource(Object key)
    {
        return resources != null && resources.containsKey(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#enlistResource(java.lang.Object,
     *      java.lang.Object)
     */
    public void bindResource(Object key, Object resource) throws TransactionException
    {
        if (resources == null) {
            resources = new HashMap();
        }
        if (resources.containsKey(key)) {
            throw new IllegalTransactionStateException(new Message(Messages.TX_RESOURCE_ALREADY_LISTED_FOR_KEY_X, key));
        }
        resources.put(key, resource);
    }
}
