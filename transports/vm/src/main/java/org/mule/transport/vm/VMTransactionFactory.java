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
import org.mule.api.transaction.TransactionFactory;

public class VMTransactionFactory implements TransactionFactory
{
    public static TransactionFactory factoryDelegate = new VMTransactionFactoryDelegate();

    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        return factoryDelegate.beginTransaction(muleContext);
    }

    public boolean isTransacted()
    {
        return factoryDelegate.isTransacted();
    }

    /**
     * sets the transaction factory to be used to create VM transactions
     */
    public static void setFactoryDelegate(TransactionFactory factoryDelegate)
    {
        VMTransactionFactory.factoryDelegate = factoryDelegate;
    }

    /**
     * Create normal VM transactions
     */
    static class VMTransactionFactoryDelegate implements TransactionFactory
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
    }

}
