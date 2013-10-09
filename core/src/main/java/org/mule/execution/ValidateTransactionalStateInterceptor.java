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
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;

class ValidateTransactionalStateInterceptor<T> implements ExecutionInterceptor<T>
{
    private final ExecutionInterceptor<T> next;
    private final TransactionConfig transactionConfig;

    public ValidateTransactionalStateInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
    }

    @Override
    public T execute(ExecutionCallback<T> callback) throws Exception
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (transactionConfig.getAction() == TransactionConfig.ACTION_NEVER && tx != null)
        {
            throw new IllegalTransactionStateException(
                    CoreMessages.transactionAvailableButActionIs("Never"));
        } else if (transactionConfig.getAction() == TransactionConfig.ACTION_ALWAYS_JOIN && tx == null)
        {
            throw new IllegalTransactionStateException(
                    CoreMessages.transactionNotAvailableButActionIs("Always Join"));
        }
        return this.next.execute(callback);
    }
}
