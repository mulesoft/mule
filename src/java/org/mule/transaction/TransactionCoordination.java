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

/**
 * <p><code>TransactionCoordination</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
        if (instance == null)
        {
            instance = new TransactionCoordination();
        }
        return instance;
    }

    public static void setInstance(TransactionCoordination txSync) throws IllegalStateException
    {
        if (instance == null || instance.txCounter == 0)
        {
            instance = txSync;
        }
        else
        {
            throw new IllegalStateException("there are currently " + instance.txCounter + "transactions associated with this manager, cannot replace the manager");
        }
    }

    public UMOTransaction getTransaction()
    {
        return (UMOTransaction) transactions.get();
    }

    public void unbindTransaction(UMOTransaction transaction) throws UMOTransactionException
    {
    	UMOTransaction oldTx = (UMOTransaction) transactions.get();
    	if (oldTx != transaction) {
    		throw new IllegalTransactionStateException("Trying to unbind an unbounded transaction");
    	}
        transactions.set(null);
        decrementCounter();
    }

    public void bindTransaction(UMOTransaction transaction) throws UMOTransactionException
    {
    	UMOTransaction oldTx = (UMOTransaction) transactions.get();
    	if (oldTx != null) {
    		throw new IllegalTransactionStateException("A transaction is already bound to the current thread");
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
