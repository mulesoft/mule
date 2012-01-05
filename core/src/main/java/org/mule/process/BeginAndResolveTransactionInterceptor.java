/*
 * $Id:AbstractExternalTransactionTestCase.java 8215 2007-09-05 16:56:51Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.process;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.TransactionCoordination;

class BeginAndResolveTransactionInterceptor<T> implements ProcessingInterceptor<T>
{
    private static final Log logger = LogFactory.getLog(BeginAndResolveTransactionInterceptor.class);
    private final ProcessingInterceptor<T> next;
    private final TransactionConfig transactionConfig;
    private final MuleContext muleContext;
    private final boolean processOnException;

    BeginAndResolveTransactionInterceptor(ProcessingInterceptor next, TransactionConfig transactionConfig, MuleContext muleContext, boolean processOnException)
    {
        this.next = next;
        this.transactionConfig = transactionConfig;
        this.muleContext = muleContext;
        this.processOnException = processOnException;
    }

    @Override
    public T execute(ProcessingCallback<T> callback) throws Exception
    {
        byte action = transactionConfig.getAction();
        boolean mustResolveTransaction = false;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (action == TransactionConfig.ACTION_ALWAYS_BEGIN
                || (action == TransactionConfig.ACTION_BEGIN_OR_JOIN && tx == null))
        {
            logger.debug("Beginning transaction");
            tx = transactionConfig.getFactory().beginTransaction(muleContext);
            mustResolveTransaction = true;
            logger.debug("Transaction successfully started: " + tx);
        }
        T result;
        try
        {
            result = next.execute(callback);
            resolveTransactionIfRequired(mustResolveTransaction);
            return result;
        }
        catch (MessagingException e)
        {
            if (processOnException)
            {
                resolveTransactionIfRequired(true);
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
