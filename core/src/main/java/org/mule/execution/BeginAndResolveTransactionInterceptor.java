/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.TransactionCoordination;

class BeginAndResolveTransactionInterceptor<T> implements ExecutionInterceptor<T>
{
    private static final Log logger = LogFactory.getLog(BeginAndResolveTransactionInterceptor.class);
    private final ExecutionInterceptor<T> next;
    private final TransactionConfig transactionConfig;
    private final MuleContext muleContext;
    private final boolean processOnException;
    private boolean mustResolveAnyTransaction;

    BeginAndResolveTransactionInterceptor(ExecutionInterceptor next, TransactionConfig transactionConfig, MuleContext muleContext, boolean processOnException, boolean mustResolveAnyTransaction)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
        this.muleContext = muleContext;
        this.processOnException = processOnException;
        this.mustResolveAnyTransaction = mustResolveAnyTransaction;
    }

    @Override
    public T execute(ExecutionCallback<T> callback) throws Exception
    {
        byte action = transactionConfig.getAction();
        boolean resolveStartedTransaction = false;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (action == TransactionConfig.ACTION_ALWAYS_BEGIN
                || (action == TransactionConfig.ACTION_BEGIN_OR_JOIN && tx == null))
        {
            logger.debug("Beginning transaction");
            tx = transactionConfig.getFactory().beginTransaction(muleContext);
            resolveStartedTransaction = true;
            logger.debug("Transaction successfully started: " + tx);
        }
        T result;
        try
        {
            result = next.execute(callback);
            resolveTransactionIfRequired(resolveStartedTransaction);
            return result;
        }
        catch (MessagingException e)
        {
            if (processOnException)
            {
                resolveTransactionIfRequired(resolveStartedTransaction || mustResolveAnyTransaction);
            }
            throw e;
        }
    }

    private void resolveTransactionIfRequired(boolean mustResolveTransaction) throws TransactionException
    {
        Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (mustResolveTransaction && transaction != null)
        {
            TransactionCoordination.getInstance().resolveTransaction();
        }
    }

}
