/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    /**
     * @deprecated For customizing the behavior of VM transport the whole {@link org.mule.util.queue.QueueManager} should be override
     * @param factoryDelegate
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
