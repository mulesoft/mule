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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;

public class TransactionCoordination
{
    protected static final Log logger = LogFactory.getLog(TransactionCoordination.class);

    private static volatile TransactionCoordination instance;

    private final ThreadLocal transactions = new ThreadLocal();
    private int txCounter = 0;

    private TransactionCoordination()
    {
        super();
    }

    public static TransactionCoordination getInstance()
    {
        if (instance == null)
        {
            setInstance(new TransactionCoordination());
        }

        return instance;
    }

    public static synchronized void setInstance(TransactionCoordination txSync)
    {
        if (instance == null)
        {
            instance = txSync;
        }

        synchronized (instance)
        {
            if (instance.txCounter != 0)
            {
                throw new IllegalStateException("there are currently " + instance.txCounter
                                + "transactions associated with this manager, cannot replace the manager");
            }
        }
    }

    public UMOTransaction getTransaction()
    {
        return (UMOTransaction)transactions.get();
    }

    public void unbindTransaction(UMOTransaction transaction) throws TransactionException
    {
        try
        {
            UMOTransaction oldTx = (UMOTransaction)transactions.get();
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
        UMOTransaction oldTx = (UMOTransaction)transactions.get();
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
