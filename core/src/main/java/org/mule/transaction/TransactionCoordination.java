/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.apache.commons.collections.ArrayStack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.DelegateTransaction;

public final class TransactionCoordination
{
    protected static final Log logger = LogFactory.getLog(TransactionCoordination.class);

    private static final TransactionCoordination instance = new TransactionCoordination();

    /**
     * This field could be static because it is a {@link ThreadLocal} and this class
     * is a singleton but, as it is used as an instance field by methods
     * {@link #getTransaction()}, {@link #unbindTransaction(Transaction)} and
     * {@link #bindTransaction(Transaction)}, it may be more consistent to have it as
     * an instance variable.
     */
    private final ThreadLocal<Transaction> transactions = new ThreadLocal<Transaction>();
    private final ThreadLocal<Transaction> suspendedTransaction = new ThreadLocal<Transaction>();
    private final ThreadLocal<ArrayStack>  isolatedTransactions = new ThreadLocal<ArrayStack>();

    /** Lock variable that is used to access {@link #txCounter}. */
    private final Object txCounterLock = new Object();

    /** The access to this field is guarded by {@link #txCounterLock}. */
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

    public void unbindTransaction(final Transaction transaction) throws TransactionException
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
            logTransactionUnbound(transaction);
        }
    }

    private void logTransactionUnbound(final Transaction transaction)
    {
        // We store the txCounter in a local variable to minimize locking
        int txCounter = 0;
        synchronized (txCounterLock)
        {
            if (this.txCounter > 0)
            {
                txCounter = --this.txCounter;
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Unbinding transaction (" + txCounter + ") " + transaction);
        }
    }

    public void bindTransaction(final Transaction transaction) throws TransactionException
    {
        Transaction oldTx = transactions.get();
        // special handling for transaction collection
        if (oldTx != null && !(oldTx instanceof TransactionCollection) && !(oldTx instanceof DelegateTransaction))
        {
            throw new IllegalTransactionStateException(CoreMessages.transactionAlreadyBound());
        }

        if (oldTx != null && oldTx instanceof DelegateTransaction)
        {
            DelegateTransaction delegateTransaction = (DelegateTransaction) oldTx;
            if (!delegateTransaction.supportsInnerTransaction(transaction))
            {
                throw new IllegalTransactionStateException(CoreMessages.transactionAlreadyBound());
            }
            return;
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
        logTransactionBound(transaction);
    }

    private void logTransactionBound(final Transaction transaction)
    {
        // We store the txCounter in a local variable to minimize locking
        int txCounter;
        synchronized (txCounterLock)
        {
            txCounter = ++this.txCounter;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Binding new transaction (" + txCounter + ") " + transaction);
        }
    }

    public void resumeXaTransactionIfAvailable()
    {
        try
        {
            Transaction tx = suspendedTransaction.get();
            if (tx != null)
            {
                resumeSuspendedTransaction();
            }
        }
        catch (TransactionException e)
        {
            logger.error("Failure resuming suspended transaction",e);
        }
    }

    public void commitCurrentTransaction()
    {
        Transaction tx = transactions.get();
        if (tx != null)
        {
            try
            {
                tx.commit();
            }
            catch (TransactionException e)
            {
                logger.error(e);
            }
        }
    }

    public void rollbackCurrentTransaction()
    {
        Transaction tx = transactions.get();
        if (tx != null)
        {
            try
            {
                tx.rollback();
            }
            catch (TransactionException e)
            {
                logger.error(e);
            }
        }
    }

    public void resolveTransaction() throws TransactionException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx.isRollbackOnly())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Transaction has been marked rollbackOnly, rolling it back: " + tx);
            }
            tx.rollback();
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Committing transaction " + tx);
            }
            tx.commit();
        }
    }

    public void suspendCurrentTransaction() throws TransactionException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (logger.isDebugEnabled())
        {
            logger.debug("Suspending " + tx);
        }

        tx.suspend();

        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully suspended " + tx);
            logger.debug("Unbinding the following TX from the current context: " + tx);
        }

        TransactionCoordination.getInstance().unbindTransaction(tx);
        suspendedTransaction.set(tx);
    }

    public void resumeSuspendedTransaction() throws TransactionException
    {
        Transaction tx = suspendedTransaction.get();
        if (logger.isDebugEnabled())
        {
            logger.debug("Re-binding and Resuming " + tx);
        }
        TransactionCoordination.getInstance().bindTransaction(tx);
        suspendedTransaction.remove();
        tx.resume();
    }

    public void clear()
    {
        suspendedTransaction.remove();
        transactions.remove();
        if (isolatedTransactions.get() != null)
        {
            isolatedTransactions.get().clear();
        }
        isolatedTransactions.remove();
    }

    public void isolateTransaction()
    {
        Transaction currentTransaction = transactions.get();
        if (currentTransaction != null)
        {
            if (isolatedTransactions.get() == null)
            {
                isolatedTransactions.set(new ArrayStack());
            }
            isolatedTransactions.get().push(transactions.get());
            transactions.set(null);
        }
    }

    public void restoreIsolatedTransaction()
    {
        if (isolatedTransactions.get() != null && !isolatedTransactions.get().isEmpty())
        {
            transactions.set((Transaction) isolatedTransactions.get().pop());
        }
    }
}
