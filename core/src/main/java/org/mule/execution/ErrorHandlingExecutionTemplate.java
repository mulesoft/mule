/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.transaction.TransactionConfig;
import org.mule.transaction.MuleTransactionConfig;

/**
* ExecutionTemplate created by this method should be used on the beginning of the execution of a chain of
* MessageProcessor that should manage exceptions.
* Should be used when:
*  An asynchronous MessageProcessor chain is being executed
*      Because of an <async> element
*      Because of an asynchronous processing strategy
*  A Flow is called using a <flow-ref> element
*
* Instance of ErrorHandlingExecutionTemplate will:
*  Route any exception to exception strategy
*
*/
public class ErrorHandlingExecutionTemplate implements ExecutionTemplate<MuleEvent>
{
    private final ExecutionInterceptor<MuleEvent> processingInterceptor;

    private ErrorHandlingExecutionTemplate(final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler)
    {
        final TransactionConfig transactionConfig = new MuleTransactionConfig();
        final boolean processTransactionOnException = false;
        ExecutionInterceptor<MuleEvent> tempExecutionInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempExecutionInterceptor = new CommitTransactionInterceptor(tempExecutionInterceptor);
        tempExecutionInterceptor = new HandleExceptionInterceptor(tempExecutionInterceptor, messagingExceptionHandler);
        tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig,muleContext, processTransactionOnException, false);
        tempExecutionInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig,processTransactionOnException);
        this.processingInterceptor = new RethrowExceptionInterceptor(tempExecutionInterceptor);
    }

    /**
     * Creates a ErrorHandlingExecutionTemplate to be used as the main enthat will route any MessagingException thrown to an exception listener
     *
     * @param muleContext MuleContext for this application
     * @param messagingExceptionHandler exception listener to execute for any MessagingException exception
     */
    public static ErrorHandlingExecutionTemplate createErrorHandlingExecutionTemplate(final MuleContext muleContext, final MessagingExceptionHandler messagingExceptionHandler)
    {
        return new ErrorHandlingExecutionTemplate(muleContext, messagingExceptionHandler);
    }

    @Override
    public MuleEvent execute(ExecutionCallback<MuleEvent> executionCallback) throws Exception
    {
        return this.processingInterceptor.execute(executionCallback, new ExecutionContext());
    }
}
