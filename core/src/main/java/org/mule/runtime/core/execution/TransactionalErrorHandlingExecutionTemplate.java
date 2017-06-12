/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;

/**
 * Creates an execution context that should be used when: - A flow execution starts because a message was received by a
 * MessageReceiver - Any other entry point of execution with no parent execution context
 * <p>
 * Created a ExecutionTemplate that will: Resolve non xa transactions created before it if the TransactionConfig action requires
 * it suspend-resume xa transaction created before it if the TransactionConfig action requires it start a transaction if required
 * by TransactionConfig action resolve transaction if was started by this TransactionTemplate route any exception to exception
 * strategy if it was not already routed to it
 */
public class TransactionalErrorHandlingExecutionTemplate implements ExecutionTemplate<Event> {

  private ExecutionInterceptor<Event> executionInterceptor;

  private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext,
                                                      MessagingExceptionHandler messagingExceptionHandler,
                                                      FlowConstruct flowConstruct, boolean resolveAnyTransaction) {
    this(muleContext, new MuleTransactionConfig(), messagingExceptionHandler, flowConstruct, resolveAnyTransaction);
  }

  private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig,
                                                      MessagingExceptionHandler messagingExceptionHandler,
                                                      FlowConstruct flowConstruct, boolean resolveAnyTransaction) {
    final boolean processTransactionOnException = true;
    ExecutionInterceptor<Event> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor = new CommitTransactionInterceptor(tempExecutionInterceptor);
    tempExecutionInterceptor = new HandleExceptionInterceptor(tempExecutionInterceptor, messagingExceptionHandler, flowConstruct);
    tempExecutionInterceptor =
        new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, muleContext,
                                                    processTransactionOnException, resolveAnyTransaction);
    tempExecutionInterceptor = new ResolvePreviousTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new SuspendXaTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                     processTransactionOnException);
    tempExecutionInterceptor = new ValidateTransactionalStateInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new IsolateCurrentTransactionInterceptor(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new ExternalTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, muleContext);
    this.executionInterceptor = new RethrowExceptionInterceptor(tempExecutionInterceptor);
  }

  private TransactionalErrorHandlingExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig,
                                                      FlowConstruct flowConstruct, boolean resolveAnyTransaction) {
    this(muleContext, transactionConfig, null, flowConstruct, resolveAnyTransaction);
  }

  /**
   * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow using no transaction
   * configuration
   *
   * @param muleContext MuleContext for this application
   * @param messagingExceptionHandler exception listener to use for any MessagingException thrown
   */
  public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext,
                                                                                        FlowConstruct flowConstruct,
                                                                                        MessagingExceptionHandler messagingExceptionHandler) {
    return new TransactionalErrorHandlingExecutionTemplate(muleContext, messagingExceptionHandler, flowConstruct, true);
  }

  /**
   * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow
   *
   * @param muleContext MuleContext for this application
   * @param transactionConfig Transaction configuration
   * @param messagingExceptionHandler Exception listener for any MessagingException thrown
   */
  public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext,
                                                                                        FlowConstruct flowConstruct,
                                                                                        TransactionConfig transactionConfig,
                                                                                        MessagingExceptionHandler messagingExceptionHandler) {
    return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, messagingExceptionHandler,
                                                           flowConstruct, true);
  }

  /**
   * Creates a TransactionalErrorHandlingExecutionTemplate to be used as first processing template in a flow using no particular
   * exception listener. Exception listener configured in the flow within this ExecutionTemplate is executed will be used.
   *
   * @param muleContext MuleContext for this application
   * @param transactionConfig Transaction configuration
   */
  public static TransactionalErrorHandlingExecutionTemplate createMainExecutionTemplate(MuleContext muleContext,
                                                                                        FlowConstruct flowConstruct,
                                                                                        TransactionConfig transactionConfig) {
    return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, flowConstruct, true);
  }

  /**
   * Creates a TransactionalErrorHandlingExecutionTemplate for inner scopes within a flow
   *
   * @param muleContext
   * @param transactionConfig
   * @return
   */
  public static TransactionalErrorHandlingExecutionTemplate createScopeExecutionTemplate(MuleContext muleContext,
                                                                                         FlowConstruct flowConstruct,
                                                                                         TransactionConfig transactionConfig,
                                                                                         MessagingExceptionHandler messagingExceptionHandler) {
    return new TransactionalErrorHandlingExecutionTemplate(muleContext, transactionConfig, messagingExceptionHandler,
                                                           flowConstruct, false);
  }


  @Override
  public Event execute(ExecutionCallback<Event> executionCallback) throws Exception {
    return this.executionInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
