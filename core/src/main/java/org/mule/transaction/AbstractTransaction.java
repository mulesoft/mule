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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This base class provides low level features for transactions.
 */
public abstract class AbstractTransaction implements UMOTransaction
{

    protected final transient Log logger = LogFactory.getLog(getClass());

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#isRollbackOnly()
     */
    public boolean isRollbackOnly() throws TransactionException
    {
        return getStatus() == STATUS_MARKED_ROLLBACK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#isBegun()
     */
    public boolean isBegun() throws TransactionException
    {
        int status = getStatus();
        return status != STATUS_NO_TRANSACTION && status != STATUS_UNKNOWN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#isRolledBack()
     */
    public boolean isRolledBack() throws TransactionException
    {
        return getStatus() == STATUS_ROLLEDBACK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#isCommitted()
     */
    public boolean isCommitted() throws TransactionException
    {
        return getStatus() == STATUS_COMMITTED;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#begin()
     */
    public void begin() throws TransactionException
    {
        logger.debug("Beginning transaction");
        doBegin();
        TransactionCoordination.getInstance().bindTransaction(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#commit()
     */
    public void commit() throws TransactionException
    {
        try
        {
            logger.debug("Committing transaction");

            if (isRollbackOnly())
            {
                throw new IllegalTransactionStateException(new Message(Messages.TX_MARKED_FOR_ROLLBACK));
            }

            doCommit();
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    public void rollback() throws TransactionException
    {
        try
        {
            logger.debug("Rolling back transaction");
            setRollbackOnly();
            doRollback();
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction(this);
        }
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

}
