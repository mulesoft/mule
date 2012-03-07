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

/**
* ProcessingTemplate created should be used on a MessageProcessor that are previously wrapper by
* TransactionalErrorHandlingProcessingTemplate or  ErrorHandlingProcessingTemplate
* Should be used when:
*  An outbound endpoint is called
*  An outbound router is called
*  Any other MessageProcessor able to manage transactions is called
* Instance of TransactionTemplate created by this method will:
*  Resolve non xa transactions created before it if the TransactionConfig action requires it
*  Suspend-Resume xa transaction created before it if the TransactionConfig action requires it
*  Start a transaction if required by TransactionConfig action
*  Resolve transaction if was started by this TransactionTemplate
*  Route any exception to exception strategy if it was not already routed to it
*
*/
public class TransactionalProcessingTemplate<T> implements ProcessingTemplate<T>
{
    private ProcessingInterceptor<T> processingInterceptor;

    /**
     * Creates a ProcessingTemplate that will manage transactional context according to configured TransactionConfig
     *
     * @param muleContext MuleContext for this application
     * @param transactionConfig transaction config for the execution context
     */
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
        tempProcessingInterceptor = new IsolateCurrentTransactionInterceptor<T>(tempProcessingInterceptor, transactionConfig);
        this.processingInterceptor = new ExternalTransactionInterceptor(tempProcessingInterceptor,transactionConfig, muleContext);
    }

    @Override
    public T execute(ProcessingCallback<T> processingCallback) throws Exception
    {
        return processingInterceptor.execute(processingCallback);
    }
}
