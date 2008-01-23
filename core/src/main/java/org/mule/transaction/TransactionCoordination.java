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

import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class TransactionCoordination
{
    protected static final Log logger = LogFactory.getLog(TransactionCoordination.class);

    private static final TransactionCoordination instance = new TransactionCoordination();

    private static final ThreadLocal transactions = new ThreadLocal();

    // @GuardedBy("this")
    private int txCounter = 0;

    /** Do not instanciate. */
    private TransactionCoordination()
    {
        super();
    }

    public static TransactionCoordination getInstance()
    {
        return instance;
    }

    public Transaction getTransaction()
    {
        return (Transaction) transactions.get();
    }

    public void unbindTransaction(Transaction transaction) throws TransactionException
    {
        try
        {
            Transaction oldTx = (Transaction) transactions.get();
            if (oldTx != null && !oldTx.equals(transaction))
            {
                throw new IllegalTransactionStateException(CoreMessages.transactionCannotUnbind());
            }
        }
        finally
        {
            transactions.set(null);

            synchronized (this)
            {
                if (txCounter > 0)
                {
                    txCounter--;
                }
            }
        }
    }

    public void bindTransaction(Transaction transaction) throws TransactionException
    {
        Transaction oldTx = (Transaction) transactions.get();
        if (oldTx != null)
        {
            throw new IllegalTransactionStateException(CoreMessages.transactionAlreadyBound());
        }

        transactions.set(transaction);

        synchronized (this)
        {
            txCounter++;

            if (logger.isDebugEnabled())
            {
                logger.debug("Binding new transaction (" + txCounter + ") " + transaction);
            }
        }
    }

}
