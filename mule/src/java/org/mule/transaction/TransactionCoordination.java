/* 
 * $Id$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;

/**
 * <p>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class TransactionCoordination
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(TransactionCoordination.class);

    private static TransactionCoordination instance;

    private int txCounter = 0;

    private ThreadLocal transactions = new ThreadLocal();

    private TransactionCoordination()
    {
        txCounter = 0;
    }

    public static TransactionCoordination getInstance()
    {
        if (instance == null) {
            instance = new TransactionCoordination();
        }
        return instance;
    }

    public static void setInstance(TransactionCoordination txSync)
    {
        if (instance == null || instance.txCounter == 0) {
            instance = txSync;
        } else {
            throw new IllegalStateException("there are currently " + instance.txCounter
                    + "transactions associated with this manager, cannot replace the manager");
        }
    }

    public UMOTransaction getTransaction()
    {
        return (UMOTransaction) transactions.get();
    }

    public void unbindTransaction(UMOTransaction transaction) throws TransactionException
    {
        try {
            UMOTransaction oldTx = (UMOTransaction) transactions.get();
            if (oldTx!=null && !oldTx.equals(transaction)) {
                throw new IllegalTransactionStateException(new Message(Messages.TX_CANT_UNBIND));
            }
        } finally {
            transactions.set(null);
            decrementCounter();
        }
    }

    public void bindTransaction(UMOTransaction transaction) throws TransactionException
    {
        UMOTransaction oldTx = (UMOTransaction) transactions.get();
        if (oldTx != null) {
            throw new IllegalTransactionStateException(new Message(Messages.TX_CANT_BIND_ALREADY_BOUND));
        }
        transactions.set(transaction);
        incrementCounter();
        logger.debug("Binding new transaction (" + txCounter + ")");
    }

    private synchronized void decrementCounter()
    {
        if (txCounter > 0) {
            txCounter--;
        }
    }

    private synchronized void incrementCounter()
    {
        txCounter++;
    }
}
