/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionConfig;

public class TransactionTemplateFactory
{
    /**
     * TransactionTemplate created by this method should be used in the beginning of a flow execution.
     * Should be used when:
     *  A flow execution starts because a message was received by a MessageReceiver
     *
     * Instance of TransactionTemplate created by this method will:
     *  Resolve non xa transactions created before it if the TransactionConfig action requires it
     *  Suspend-Resume xa transaction created before it if the TransactionConfig action requires it
     *  Start a transaction if required by TransactionConfig action
     *  Resolve transaction if was started by this TransactionTemplate
     *  Route any exception to exception strategy if it was not already routed to it
     */
    public static <T> TransactionTemplate createMainTransactionTemplate(TransactionConfig transactionConfig, MuleContext muleContext)
    {
        TransactionTemplate<T> transactionTemplate = new TransactionTemplate<T>(transactionConfig, muleContext);
        TransactionTemplate.TransactionInterceptor<T> transactionInterceptorChain = transactionTemplate.new HandleExceptionInterceptor(transactionTemplate.new ExecuteCallbackInterceptor());
        transactionInterceptorChain = addTransactionInterceptorsIfRequired(transactionConfig, transactionTemplate, transactionInterceptorChain);
        transactionTemplate.setTransactionInterceptor(transactionInterceptorChain);
        return transactionTemplate;
    }

    /**
     * TransactionTemplate created by this method should be used on the beginning of the execution of a chain of
     * MessageProcessor that should manage exceptions.
     * Should be used when:
     *  An asynchronous MessageProcessor chain is being executed
     *      Because of an <async> element
     *      Because of an asynchronous processing strategy
     *  A Flow is called using a <flow-ref> element
     *
     * Instance of TransactionTemplate created by this method will:
     *  Route any exception to exception strategy if it was not already routed to it
     */
    public static  <T> TransactionTemplate createExceptionHandlingTransactionTemplate(MuleContext muleContext)
    {
        TransactionTemplate<T> transactionTemplate = new TransactionTemplate<T>(muleContext);
        TransactionTemplate.TransactionInterceptor<T> transactionInterceptorChain = transactionTemplate.new HandleExceptionInterceptor(transactionTemplate.new ExecuteCallbackInterceptor());
        transactionTemplate.setTransactionInterceptor(transactionInterceptorChain);
        return transactionTemplate;
    }

    /**
     * TransactionTemplate created by this method should be used on a MessageProcessor that are previously wrapper by
     * TransactionTemplateFactory.createMainTransactionTemplate(..) or  TransactionTemplateFactory.createExceptionHandlingTransactionTemplate(..).
     * Should be used when:
     *  An outbound endpoint is called
     *  An outbound router is called
     *  Any other MessageProcessor able to manage transactions is called
     * Instance of TransactionTemplate created by this method will:
     *  Resolve non xa transactions created before it if the TransactionConfig action requires it
     *  Suspend-Resume xa transaction created before it if the TransactionConfig action requires it
     *  Start a transaction if required by TransactionConfig action
     *  Resolve transaction if was started by this TransactionTemplate
     */
    public static <T> TransactionTemplate createNestedTransactionTemplate(TransactionConfig transactionConfig, MuleContext muleContext)
    {
        TransactionTemplate<T> transactionTemplate = new TransactionTemplate<T>(transactionConfig, muleContext);
        TransactionTemplate.TransactionInterceptor<T> transactionInterceptorChain = transactionTemplate.new ExecuteCallbackInterceptor();
        transactionInterceptorChain = addTransactionInterceptorsIfRequired(transactionConfig, transactionTemplate, transactionInterceptorChain);
        transactionTemplate.setTransactionInterceptor(transactionInterceptorChain);
        return transactionTemplate;
    }

    private static <T> TransactionTemplate.TransactionInterceptor<T> addTransactionInterceptorsIfRequired(TransactionConfig transactionConfig, TransactionTemplate<T> transactionTemplate, TransactionTemplate.TransactionInterceptor<T> transactionInterceptorChain)
    {
        if (transactionConfig != null && transactionConfig.getAction() != TransactionConfig.ACTION_INDIFFERENT)
        {
            transactionInterceptorChain = transactionTemplate.new ExternalTransactionInterceptor(
                    transactionTemplate.new ValidateTransactionalStateInterceptor(
                            transactionTemplate.new SuspendXaTransactionInterceptor(
                                    transactionTemplate.new ResolveTransactionInterceptor(
                                            transactionTemplate.new BeginTransactionInterceptor(transactionInterceptorChain)))));
        }
        return transactionInterceptorChain;
    }
}
