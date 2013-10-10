/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;

/**
 * Transaction factory for DelegateTransaction. Used for
 * transactional element since transaction type is not
 * known until the first transactional message processor is executed.
 */
public class DelegateTransactionFactory implements TransactionFactory
{
    @Override
    public Transaction beginTransaction(MuleContext muleContext) throws TransactionException
    {
        DelegateTransaction delegateTransaction = new DelegateTransaction(muleContext);
        delegateTransaction.begin();
        return delegateTransaction;
    }

    @Override
    public boolean isTransacted()
    {
        return true;
    }

}
