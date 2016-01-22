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
*
* Creates an execution context that should be used when:
* - A flow execution starts because a message was received by a MessageReceiver
* - Any other entry point of execution with no parent execution context
*
* Created a ExecutionTemplate that will:
*  Resolve non xa transactions created before it if the TransactionConfig action requires it
*  suspend-resume xa transaction created before it if the TransactionConfig action requires it
*  start a transaction if required by TransactionConfig action
*  resolve transaction if was started by this TransactionTemplate
*  route any exception to exception strategy if it was not already routed to it
*/
public class TransactionalErrorHandlingExecutionTemplate implements ExecutionTemplate<MuleEvent>
{
    private ExecutionInterceptor<MuleEvent> executionInterceptor;

    private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext, MessagingExceptionHandler messagingExceptionHandler, boolean resolveAnyTransaction)
    {
        this(muleContext, new MuleTransactionConfig(), messagingExceptionHandler, resolveAnyTransaction);
    }

    private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig, MessagingExceptionHandler messagingExceptionHandler, boolean resolveAnyTransaction)
    {
        final boolean processTransactionOnException = true;
        ExecutionInterceptor<MuleEvent> tempExecutionInterceptor = new ExecuteCallbackInterceptor<MuleEvent>();
        tempExecutionInterceptor = new CommitTransactionInterceptor(tempExecutionInterceptor);
        tempExecutionInterceptor = new HandleExceptionInterceptor(tempExecutionInterceptor, messagingExceptionHandler);
        tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig,muleContext, processTransactionOnException, resolveAnyTransaction);
        tempExecutionInterceptor = new ResolvePreviousTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig);
        tempExecutionInterceptor = new SuspendXaTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig,processTransactionOnException);
        tempExecutionInterceptor = new ValidateTransactionalStateInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig);
        tempExecutionInterceptor = new IsolateCurrentTransactionInterceptor(tempExecutionInterceptor, transactionConfig);
        tempExecutionInterceptor = new ExternalTransactionInterceptor<MuleEvent>(tempExecutionInterceptor,transactionConfig, muleContext);
        this.executionInterceptor = new RethrowExceptionInterceptor(tempExecutionInterceptor);
    }

    private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig, boolean resolveAnyTransaction)
    {
        this(muleContext, transactionConfig, null, resolveAnyTransaction);
    }

    /**
     * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow using no transaction configuration
     *
     * @param muleContext MuleContext for this application
     * @param messagingExceptionHandler exception listener to use for any MessagingException thrown
     */
    public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext, MessagingExceptionHandler messagingExceptionHandler)
    {
        return new TransactionalErrorHandlingExecutionTemplate(muleContext, messagingExceptionHandler, true);
    }

    /**
     * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow
     *
     * @param muleContext MuleContext for this application
     * @param transactionConfig Transaction configuration
     * @param messagingExceptionHandler Exception listener for any MessagingException thrown
     */
    public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig, MessagingExceptionHandler messagingExceptionHandler)
    {
        return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, messagingExceptionHandler, true);
    }

    /**
     * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow using no particular exception listener.
     * Exception listener configured in the flow within this ExecutionTemplate is executed will be used.
     *
     * @param muleContext MuleContext for this application
     * @param transactionConfig Transaction configuration
     */
    public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig)
    {
        return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, true);
    }

    /**
     * Creates a TransactionalErrorHandlingExecutionTemplate for inner scopes within a flow
     *
     * @param muleContext
     * @param transactionConfig
     * @return
     */
    public static TransactionalErrorHandlingExecutionTemplate createScopeExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig, MessagingExceptionHandler messagingExceptionHandler)
    {
        return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, messagingExceptionHandler, false);
    }


    @Override
    public MuleEvent execute(ExecutionCallback<MuleEvent> executionCallback) throws Exception
    {
        return this.executionInterceptor.execute(executionCallback, new ExecutionContext());
    }
}
