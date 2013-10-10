/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MessagingException;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.TransactionCoordination;

public class SuspendXaTransactionInterceptor<T> implements ExecutionInterceptor<T>
{
    private static final Log logger = LogFactory.getLog(SuspendXaTransactionInterceptor.class);
    private final ExecutionInterceptor<T> next;
    private final TransactionConfig transactionConfig;
    private final boolean processOnException;

    public SuspendXaTransactionInterceptor(ExecutionInterceptor<T> next, TransactionConfig transactionConfig, boolean processOnException)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
        this.processOnException = processOnException;
    }

    @Override
    public T execute(ExecutionCallback<T> callback) throws Exception
    {
        Transaction suspendedXATx = null;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        byte action = transactionConfig.getAction();
        if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
                && tx != null && tx.isXA())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("suspending XA tx " + action + ", " + "current TX: " + tx);
            }
            suspendedXATx = tx;
            suspendXATransaction(suspendedXATx);
        }
        try
        {
            T result = next.execute(callback);
            resumeXaTransactionIfRequired(suspendedXATx);
            return result;
        }
        catch (MessagingException e)
        {
            if (processOnException)
            {
                TransactionCoordination.getInstance().resumeXaTransactionIfAvailable();
            }
            throw e;
        }
    }

    protected void suspendXATransaction(Transaction tx) throws TransactionException
    {
        TransactionCoordination.getInstance().suspendCurrentTransaction();
    }

    protected void resumeXATransaction(Transaction tx) throws TransactionException
    {
        TransactionCoordination.getInstance().resumeSuspendedTransaction();
    }

    private void resumeXaTransactionIfRequired(Transaction suspendedXATx) throws TransactionException
    {
        if (suspendedXATx != null)
        {
            resumeXATransaction(suspendedXATx);
        }
    }

}

