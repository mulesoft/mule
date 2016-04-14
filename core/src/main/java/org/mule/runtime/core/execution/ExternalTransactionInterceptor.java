/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MuleContext;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionFactory;
import org.mule.transaction.TransactionCoordination;

class ExternalTransactionInterceptor<T> implements ExecutionInterceptor<T>
{
    private final ExecutionInterceptor<T> next;
    private TransactionConfig transactionConfig;
    private MuleContext muleContext;

    public ExternalTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig, MuleContext muleContext)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
        this.muleContext = muleContext;
    }

    @Override
    public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception
    {
        Transaction joinedExternal = null;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        try
        {
            if (tx == null && muleContext != null && transactionConfig != null && transactionConfig.isInteractWithExternal())
            {

                TransactionFactory tmFactory = transactionConfig.getFactory();
                if (tmFactory instanceof ExternalTransactionAwareTransactionFactory)
                {
                    ExternalTransactionAwareTransactionFactory externalTransactionFactory =
                            (ExternalTransactionAwareTransactionFactory) tmFactory;
                    joinedExternal = tx = externalTransactionFactory.joinExternalTransaction(muleContext);
                }
            }
            return next.execute(callback, executionContext);
        }
        finally
        {
            if (joinedExternal != null)
            {
                TransactionCoordination.getInstance().unbindTransaction(joinedExternal);
            }
        }
    }
}
