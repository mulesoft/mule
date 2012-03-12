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

/**
* ProcessingTemplate created by this method should be used on the beginning of the execution of a chain of
* MessageProcessor that should manage exceptions.
* Should be used when:
*  An asynchronous MessageProcessor chain is being executed
*      Because of an <async> element
*      Because of an asynchronous processing strategy
*  A Flow is called using a <flow-ref> element
*
* Instance of ErrorHandlingProcessingTemplate will:
*  Route any exception to exception strategy
*
*/
public class ErrorHandlingProcessingTemplate implements ProcessingTemplate<MuleEvent>
{
    private final ProcessingInterceptor<MuleEvent> processingInterceptor;

    private ErrorHandlingProcessingTemplate(final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler)
    {
        final TransactionConfig transactionConfig = new MuleTransactionConfig();
        final boolean processTransactionOnException = false;
        ProcessingInterceptor<MuleEvent> tempProcessingInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempProcessingInterceptor = new HandleExceptionInterceptor(tempProcessingInterceptor, messagingExceptionHandler);
        tempProcessingInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,muleContext, processTransactionOnException, false);
        tempProcessingInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,processTransactionOnException);
        this.processingInterceptor = new RethrowExceptionInterceptor(tempProcessingInterceptor);
    }

    /**
     * Creates a ErrorHandlingProcessingTemplate to be used as the main enthat will route any MessagingException thrown to an exception listener
     *
     * @param muleContext MuleContext for this application
     * @param messagingExceptionHandler exception listener to execute for any MessagingException exception
     */
    public static ErrorHandlingProcessingTemplate createErrorHandlingProcessingTemplate(final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler)
    {
        return new ErrorHandlingProcessingTemplate(muleContext, messagingExceptionHandler);
    }

    @Override
    public MuleEvent execute(ProcessingCallback<MuleEvent> processingCallback) throws Exception
    {
        return this.processingInterceptor.execute(processingCallback);
    }
}
