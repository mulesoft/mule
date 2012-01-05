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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transaction.TransactionConfig;
import org.mule.transaction.MuleTransactionConfig;

public class TransactionalProcessingTemplate<T> implements ProcessingTemplate<T>
{
    private ProcessingInterceptor<T> processingInterceptor;

    public TransactionalProcessingTemplate(MuleContext muleContext, TransactionConfig transactionConfig)
    {
        if (transactionConfig == null)
        {
            transactionConfig = new MuleTransactionConfig();
        }
        final boolean processTransactionOnException = false;
        ProcessingInterceptor<T> tempProcessingInterceptor = new ExecuteCallbackInterceptor<T>();
        tempProcessingInterceptor = new BeginAndResolveTransactionInterceptor<T>(tempProcessingInterceptor,transactionConfig,muleContext, processTransactionOnException);
        tempProcessingInterceptor = new ResolvePreviousTransactionInterceptor<T>(tempProcessingInterceptor,transactionConfig);
        tempProcessingInterceptor = new SuspendXaTransactionInterceptor<T>(tempProcessingInterceptor,transactionConfig,processTransactionOnException);
        tempProcessingInterceptor = new ValidateTransactionalStateInterceptor<T>(tempProcessingInterceptor,transactionConfig);
        this.processingInterceptor = new ExternalTransactionInterceptor<T>(tempProcessingInterceptor,transactionConfig, muleContext);
    }

    @Override
    public T execute(ProcessingCallback<T> processingCallback) throws Exception
    {
        return processingInterceptor.execute(processingCallback);
    }
}
