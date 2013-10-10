/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public T execute(ExecutionCallback<T> callback) throws Exception
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
            return next.execute(callback);
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
