/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.TransactionCoordination;

class ResolvePreviousTransactionInterceptor<T> implements ExecutionInterceptor<T>
{
    private static final Log logger = LogFactory.getLog(ResolvePreviousTransactionInterceptor.class);
    final private ExecutionInterceptor<T> next;
    private TransactionConfig transactionConfig;

    public ResolvePreviousTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
    }

    @Override
    public T execute(ExecutionCallback<T> callback) throws Exception
    {
        byte action = transactionConfig.getAction();
        Transaction transactionBeforeTemplate = TransactionCoordination.getInstance().getTransaction();
        if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
                && transactionBeforeTemplate != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(action + ", " + "current TX: " + transactionBeforeTemplate);
            }

            resolveTransaction(transactionBeforeTemplate);
        }
        return next.execute(callback);
    }

    protected void resolveTransaction(Transaction tx) throws TransactionException
    {
        TransactionCoordination.getInstance().resolveTransaction();
    }
}
