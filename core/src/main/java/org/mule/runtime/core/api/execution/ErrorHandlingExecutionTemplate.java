/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.execution.BeginAndResolveTransactionInterceptor;
import org.mule.runtime.core.internal.execution.CommitTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ExecuteCallbackInterceptor;
import org.mule.runtime.core.internal.execution.ExecutionContext;
import org.mule.runtime.core.internal.execution.ExecutionInterceptor;
import org.mule.runtime.core.internal.execution.HandleExceptionInterceptor;
import org.mule.runtime.core.internal.execution.RethrowExceptionInterceptor;
import org.mule.runtime.core.internal.execution.SuspendXaTransactionInterceptor;

/**
 * ExecutionTemplate created by this method should be used on the beginning of the execution of a chain of {@link Processor}s that
 * should manage exceptions. Should be used when: An asynchronous MessageProcessor chain is being executed Because of an
 * {@code <async>} element Because of an asynchronous processing strategy A Flow is called using a {@code <flow-ref>} element.
 * <p>
 * Instance of ErrorHandlingExecutionTemplate will: Route any exception to exception strategy.
 */
public class ErrorHandlingExecutionTemplate implements ExecutionTemplate<Event> {

  private final ExecutionInterceptor<Event> processingInterceptor;

  private ErrorHandlingExecutionTemplate(final MuleContext muleContext,
                                         final FlowConstruct flowConstruct,
                                         final MessagingExceptionHandler messagingExceptionHandler) {
    final TransactionConfig transactionConfig = new MuleTransactionConfig();
    final boolean processTransactionOnException = false;
    ExecutionInterceptor<Event> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor = new CommitTransactionInterceptor(tempExecutionInterceptor);
    tempExecutionInterceptor = new HandleExceptionInterceptor(tempExecutionInterceptor, messagingExceptionHandler, flowConstruct);
    tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                           muleContext, processTransactionOnException, false);
    tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                           muleContext, processTransactionOnException, false);
    tempExecutionInterceptor =
        new SuspendXaTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, processTransactionOnException);
    this.processingInterceptor = new RethrowExceptionInterceptor(tempExecutionInterceptor);
  }

  /**
   * Creates a ErrorHandlingExecutionTemplate to be used as the main enthat will route any MessagingException thrown to an
   * exception listener
   *
   * @param muleContext MuleContext for this application
   * @param flowConstruct
   * @param messagingExceptionHandler exception listener to execute for any MessagingException exception
   */
  public static ErrorHandlingExecutionTemplate createErrorHandlingExecutionTemplate(final MuleContext muleContext,
                                                                                    final FlowConstruct flowConstruct,
                                                                                    final MessagingExceptionHandler messagingExceptionHandler) {
    return new ErrorHandlingExecutionTemplate(muleContext, flowConstruct, messagingExceptionHandler);
  }

  @Override
  public Event execute(ExecutionCallback<Event> executionCallback) throws Exception {
    return this.processingInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
