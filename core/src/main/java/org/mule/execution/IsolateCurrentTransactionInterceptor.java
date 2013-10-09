/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.transaction.TransactionCoordination;

public class IsolateCurrentTransactionInterceptor<T> implements ExecutionInterceptor<T>
{
    private ExecutionInterceptor<T> next;
    private TransactionConfig transactionConfig;

    public IsolateCurrentTransactionInterceptor(ExecutionInterceptor<T> nextProcessingInterceptor, TransactionConfig transactionConfig)
    {
        this.next = nextProcessingInterceptor;
        this.transactionConfig = transactionConfig;
    }


    @Override
    public T execute(ExecutionCallback<T> muleEventProcessingCallback) throws Exception
    {
        boolean transactionIsolated = false;
        try
        {
            if (transactionConfig.getAction() == TransactionConfig.ACTION_NOT_SUPPORTED)
            {
                Transaction transaction = TransactionCoordination.getInstance().getTransaction();
                if (transaction != null)
                {
                    TransactionCoordination.getInstance().isolateTransaction();
                    transactionIsolated = true;
                }
            }
            return next.execute(muleEventProcessingCallback);
        }
        finally 
        {
            if (transactionIsolated)
            {
                TransactionCoordination.getInstance().restoreIsolatedTransaction();
            }
        }
    }
}
