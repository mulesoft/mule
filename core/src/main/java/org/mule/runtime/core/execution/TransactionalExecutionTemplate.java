/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

/**
 * ExecutionTemplate created should be used on a MessageProcessor that are previously wrapper by
 * TransactionalErrorHandlingExecutionTemplate or ErrorHandlingExecutionTemplate Should be used when: An outbound endpoint is
 * called An outbound router is called Any other MessageProcessor able to manage transactions is called Instance of
 * TransactionTemplate created by this method will: Resolve non xa transactions created before it if the TransactionConfig action
 * requires it Suspend-Resume xa transaction created before it if the TransactionConfig action requires it Start a transaction if
 * required by TransactionConfig action Resolve transaction if was started by this TransactionTemplate Route any exception to
 * exception strategy if it was not already routed to it
 *
 */
public class TransactionalExecutionTemplate<T> implements ExecutionTemplate<T> {

  private ExecutionInterceptor<T> executionInterceptor;


  private TransactionalExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig) {
    if (transactionConfig == null) {
      transactionConfig = new MuleTransactionConfig();
    }
    final boolean processTransactionOnException = false;
    ExecutionInterceptor<T> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                           muleContext, processTransactionOnException, false);
    tempExecutionInterceptor = new ResolvePreviousTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor =
        new SuspendXaTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, processTransactionOnException);
    tempExecutionInterceptor = new ValidateTransactionalStateInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new IsolateCurrentTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
    this.executionInterceptor = new ExternalTransactionInterceptor(tempExecutionInterceptor, transactionConfig, muleContext);
  }

  /**
   * Creates a ExecutionTemplate that will manage transactional context according to configured TransactionConfig
   *
   * @param muleContext MuleContext for this application
   * @param transactionConfig transaction config for the execution context
   */
  public static <T> TransactionalExecutionTemplate<T> createTransactionalExecutionTemplate(MuleContext muleContext,
                                                                                           TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(muleContext, transactionConfig);
  }

  @Override
  public T execute(ExecutionCallback<T> executionCallback) throws Exception {
    return executionInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
