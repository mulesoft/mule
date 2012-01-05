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

public class ErrorHandlingProcessingTemplate implements ProcessingTemplate<MuleEvent>
{
    private final ProcessingInterceptor<MuleEvent> processingInterceptor;

    public ErrorHandlingProcessingTemplate(final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler)
    {
        final TransactionConfig transactionConfig = new MuleTransactionConfig();
        final boolean processTransactionOnException = false;
        ProcessingInterceptor<MuleEvent> tempProcessingInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempProcessingInterceptor = new HandleExceptionInterceptor(tempProcessingInterceptor, messagingExceptionHandler);
        tempProcessingInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,muleContext, processTransactionOnException);
        tempProcessingInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,processTransactionOnException);
        this.processingInterceptor = new RethrowExceptionInterceptor(tempProcessingInterceptor);
    }

    @Override
    public MuleEvent execute(ProcessingCallback<MuleEvent> processingCallback) throws Exception
    {
        return this.processingInterceptor.execute(processingCallback);
    }
}
