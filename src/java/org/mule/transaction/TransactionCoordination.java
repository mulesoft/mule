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
import org.mule.transaction.constraints.ConstraintFilter;
import org.mule.umo.UMOTransaction;

/**
 * <p><code>TransactionCoordination</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TransactionCoordination
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(TransactionCoordination.class);

    private static TransactionCoordination instance;

    private static int txCounter = 0;

    private static ThreadLocal transactions = new ThreadLocal();

    private TransactionCoordination()
    {
        transactions = new ThreadLocal();
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
        if (txCounter == 0)
        {
            instance = txSync;
        }
        else
        {
            throw new IllegalStateException("there are currently " + txCounter + "transactions associated with this manager, cannot replace the manager");
        }
    }

    public final TransactionProxy getTransactionProxy()
    {
        return (TransactionProxy) transactions.get();
    }

    public final UMOTransaction getTransaction()
    {
        TransactionProxy trans = (TransactionProxy) transactions.get();
        if(trans != null)
        {
            return trans.getTransaction();
        } else {
            return null;
        }
    }

    public final TransactionProxy unbindTransaction()
    {
        TransactionProxy trans = (TransactionProxy) transactions.get();

        transactions.set(null);
        decrementCounter();
        return trans;
    }

    public final void bindTransaction(TransactionProxy transaction)
    {
        TransactionProxy proxy = (TransactionProxy)transactions.get();
        if(proxy != null) {
            logger.debug("Binding transaction to existing (" + txCounter + ")");
            proxy.setTransaction(transaction.getTransaction());
        } else {
            transactions.set(transaction);
            incrementCounter();
            logger.debug("Binding new transaction (" + txCounter + ")");
        }
    }

    public final TransactionProxy bindTransaction(UMOTransaction transaction, ConstraintFilter constraint)
    {
        TransactionProxy proxy = (TransactionProxy)transactions.get();
        if(proxy != null) {
            logger.debug("Binding transaction to existing (" + txCounter + ")");
            proxy.setTransaction(transaction);
        } else {
            proxy = new TransactionProxy(transaction,  constraint);
            transactions.set(proxy);
            incrementCounter();
            logger.debug("Binding new transaction (" + txCounter + ")");
        }
        return proxy;
    }

    public Object getTransactionSession()
    {
        UMOTransaction tx = getTransaction();
        if (tx != null)
        {
            return tx.getResource();
        }
        return null;
    }

    private final synchronized void decrementCounter()
    {
        if(txCounter > 0) txCounter--;
    }

    private final synchronized void incrementCounter()
    {
        txCounter++;
    }
}
