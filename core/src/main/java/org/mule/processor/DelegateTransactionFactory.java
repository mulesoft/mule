/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
