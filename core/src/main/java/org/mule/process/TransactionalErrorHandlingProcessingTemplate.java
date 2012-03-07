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
*
* Creates a processing context that should be used when:
* - A flow execution starts because a message was received by a MessageReceiver
* - Any other entry point of execution with no parent execution context
*
* Created a ProcessingTemplate that will:
*  Resolve non xa transactions created before it if the TransactionConfig action requires it
*  suspend-resume xa transaction created before it if the TransactionConfig action requires it
*  start a transaction if required by TransactionConfig action
*  resolve transaction if was started by this TransactionTemplate
*  route any exception to exception strategy if it was not already routed to it
*/
public class TransactionalErrorHandlingProcessingTemplate implements ProcessingTemplate<MuleEvent>
{
    private ProcessingInterceptor<MuleEvent> processingInterceptor;

    /**
     * Creates a TransactionalErrorHandlingProcessingTemplate using no transaction configuration
     *
     * @param muleContext MuleContext for this application
     * @param messagingExceptionHandler exception listener to use for any MessagingException thrown
     */
    public TransactionalErrorHandlingProcessingTemplate(MuleContext muleContext, MessagingExceptionHandler messagingExceptionHandler)
    {
        this(muleContext, new MuleTransactionConfig(), messagingExceptionHandler);
    }

    /**
     * Creates a TransactionalErrorHandlingProcessingTemplate
     *
     * @param muleContext MuleContext for this application
     * @param transactionConfig Transaction configuration
     * @param messagingExceptionHandler Exception listener for any MessagingException thrown
     */
    public TransactionalErrorHandlingProcessingTemplate(MuleContext muleContext, TransactionConfig transactionConfig, MessagingExceptionHandler messagingExceptionHandler)
    {
        final boolean processTransactionOnException = true;
        ProcessingInterceptor<MuleEvent> tempProcessingInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempProcessingInterceptor = new HandleExceptionInterceptor(tempProcessingInterceptor, messagingExceptionHandler);
        tempProcessingInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,muleContext, processTransactionOnException);
        tempProcessingInterceptor = new ResolvePreviousTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig);
        tempProcessingInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig,processTransactionOnException);
        tempProcessingInterceptor = new ValidateTransactionalStateInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig);
        tempProcessingInterceptor = new IsolateCurrentTransactionInterceptor(tempProcessingInterceptor, transactionConfig);
        tempProcessingInterceptor = new ExternalTransactionInterceptor<MuleEvent>(tempProcessingInterceptor,transactionConfig, muleContext);
        this.processingInterceptor = new RethrowExceptionInterceptor(tempProcessingInterceptor);
    }

    /**
     * Creates a TransactionalErrorHandlingProcessingTemplate using no particular exception listener.
     * Exception listener configured in the flow within this ProcessingTemplate is executed will be used.
     *
     * @param muleContext MuleContext for this application
     * @param transactionConfig Transaction configuration
     */
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
