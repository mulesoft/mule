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
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.transaction.TransactionConfig;
import org.mule.transaction.MuleTransactionConfig;

public class TransactionalErrorHandlingProcessingTemplate implements ProcessingTemplate<MuleEvent>
{
    private ProcessingInterceptor<MuleEvent> processingInterceptor;

    public TransactionalErrorHandlingProcessingTemplate(MuleContext muleContext, MessagingExceptionHandler messagingExceptionHandler)
    {
        this(muleContext, new MuleTransactionConfig(), messagingExceptionHandler);
    }

    public TransactionalErrorHandlingProcessingTemplate(MuleContext muleContext, TransactionConfig transactionConfig, MessagingExceptionHandler messagingExceptionHandler)
    {
        final boolean processTransactionOnException = true;
        ProcessingInterceptor<MuleEvent> tempProcessingInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempProcessingInterceptor = new HandleExceptionInterceptor(tempProcessingInterceptor, messagingExceptionHandler);
        tempProcessingInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,muleContext, processTransactionOnException);
        tempProcessingInterceptor = new ResolvePreviousTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig);
        tempProcessingInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,processTransactionOnException);
        tempProcessingInterceptor = new ValidateTransactionalStateInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig);
        tempProcessingInterceptor = new ExternalTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig, muleContext);
        this.processingInterceptor = new RethrowExceptionInterceptor(tempProcessingInterceptor);
    }

    public TransactionalErrorHandlingProcessingTemplate(MuleContext muleContext, TransactionConfig transactionConfig)
    {
        this(muleContext, transactionConfig, null);
    }

    @Override
    public MuleEvent execute(ProcessingCallback<MuleEvent> processingCallback) throws Exception
    {
        return this.processingInterceptor.execute(processingCallback);
    }
}
