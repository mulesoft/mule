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

    public UMOTransaction getTransaction()
    {
        return (UMOTransaction) transactions.get();
    }

    public void unbindTransaction(UMOTransaction transaction) throws TransactionException
    {
        try
        {
            UMOTransaction oldTx = (UMOTransaction) transactions.get();
            if (oldTx != null && !oldTx.equals(transaction))
            {
                throw new IllegalTransactionStateException(new Message(Messages.TX_CANT_UNBIND));
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

    public void bindTransaction(UMOTransaction transaction) throws TransactionException
    {
        UMOTransaction oldTx = (UMOTransaction) transactions.get();
        if (oldTx != null)
        {
            throw new IllegalTransactionStateException(new Message(Messages.TX_CANT_BIND_ALREADY_BOUND));
        }

        transactions.set(transaction);

        synchronized (this)
        {
            txCounter++;

            if (logger.isDebugEnabled())
            {
                logger.debug("Binding new transaction (" + txCounter + ")");
            }
        }
    }

}
