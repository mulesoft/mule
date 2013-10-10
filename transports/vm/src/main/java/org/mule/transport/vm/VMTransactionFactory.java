/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.UniversalTransactionFactory;

public class VMTransactionFactory implements UniversalTransactionFactory
{
    public static UniversalTransactionFactory factoryDelegate = new VMTransactionFactoryDelegate();

    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        return factoryDelegate.beginTransaction(muleContext);
    }

    public boolean isTransacted()
    {
        return factoryDelegate.isTransacted();
    }

    /**
     * sets the transaction factory to be used to create VM transactions.  This must also be an UnboundTransactionFactory
     */
    public static void setFactoryDelegate(UniversalTransactionFactory factoryDelegate)
    {
        VMTransactionFactory.factoryDelegate = factoryDelegate;
    }

    @Override
    public Transaction createUnboundTransaction(MuleContext muleContext) throws TransactionException
    {
        return factoryDelegate.createUnboundTransaction(muleContext);
    }

    /**
     * Create normal VM transactions
     */
    static class VMTransactionFactoryDelegate implements UniversalTransactionFactory
    {
        public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
        {
            VMTransaction tx = new VMTransaction(muleContext);
            tx.begin();
            return tx;
        }

        public boolean isTransacted()
        {
            return true;
        }

        @Override
        public Transaction createUnboundTransaction(MuleContext muleContext) throws TransactionException
        {
            return new VMTransaction(muleContext, false);
        }
    }

}
