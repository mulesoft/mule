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

    private static final ThreadLocal<Transaction> transactions = new ThreadLocal<Transaction>();

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
        return transactions.get();
    }

    public void unbindTransaction(Transaction transaction) throws TransactionException
    {
        Transaction oldTx = transactions.get();

        if (oldTx instanceof TransactionCollection)
        {
            // if there are more in-flight aggregated transactions, do nothing yet
            if (!((TransactionCollection) oldTx).getTxCollection().isEmpty())
            {
                return;
            }
        }

        try
        {
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
        Transaction oldTx = transactions.get();
        // special handling for transaction collection
        if (oldTx != null && !(oldTx instanceof TransactionCollection))
        {
            throw new IllegalTransactionStateException(CoreMessages.transactionAlreadyBound());
        }

        if (oldTx instanceof TransactionCollection)
        {
            TransactionCollection txCollection = (TransactionCollection) oldTx;
            if (txCollection.getTxCollection().contains(transaction)) {
                // TODO improve the error message with more TX details
                throw new IllegalTransactionStateException(CoreMessages.transactionAlreadyBound());
            }
            else
            {
                // will be aggregated next
                return;
            }
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


        //
    }

}
