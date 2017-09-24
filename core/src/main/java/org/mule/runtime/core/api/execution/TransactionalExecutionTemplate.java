/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.execution.BeginAndResolveTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ExecuteCallbackInterceptor;
import org.mule.runtime.core.internal.execution.ExecutionContext;
import org.mule.runtime.core.internal.execution.ExecutionInterceptor;
import org.mule.runtime.core.internal.execution.ExternalTransactionInterceptor;
import org.mule.runtime.core.internal.execution.IsolateCurrentTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ResolvePreviousTransactionInterceptor;
import org.mule.runtime.core.internal.execution.SuspendXaTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ValidateTransactionalStateInterceptor;

/**
 * ExecutionTemplate created should be used on a MessageProcessor that are previously wrapper by
 * {@link TransactionalExecutionTemplate} should be used when:
 * <ul>
 * <li>An outbound endpoint is called.</li>
 * <li>An outbound router is called.</li>
 * <li>Other MessageProcessor able to manage transactions is called.</li>
 * </ul>
 * Any Instance of TransactionTemplate created by this method will:
 * <ul>
 * <li>Resolve non xa transactions created before it if the TransactionConfig action requires.</li>
 * <li>Suspend-Resume xa transaction created before it if the TransactionConfig action requires it.</li>
 * <li>Start a transaction if required by TransactionConfig action.</li>
 * <li><Resolve transaction if was started by this TransactionTemplate.</li>
 * </ul>
 *
 */
public class TransactionalExecutionTemplate<T> implements ExecutionTemplate<T> {

  private ExecutionInterceptor<T> executionInterceptor;


  private TransactionalExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig) {
    this(muleContext, transactionConfig, true);
  }

  private TransactionalExecutionTemplate(MuleContext muleContext, TransactionConfig transactionConfig,
                                         boolean resolveAnyTransaction) {
    if (transactionConfig == null) {
      transactionConfig = new MuleTransactionConfig();
    }
    final boolean processTransactionOnException = true;
    ExecutionInterceptor<CoreEvent> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor =
        new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, muleContext,
                                                    processTransactionOnException, resolveAnyTransaction);
    tempExecutionInterceptor = new ResolvePreviousTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new SuspendXaTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                     processTransactionOnException);
    tempExecutionInterceptor = new ValidateTransactionalStateInterceptor<>(tempExecutionInterceptor, transactionConfig);
    tempExecutionInterceptor = new IsolateCurrentTransactionInterceptor(tempExecutionInterceptor, transactionConfig);
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

  /**
   * Creates a TransactionalExecutionTemplate for inner scopes within a flow
   *
   * @param muleContext
   * @param transactionConfig
   * @return
   */
  public static TransactionalExecutionTemplate createScopeTransactionalExecutionTemplate(MuleContext muleContext,
                                                                                         TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate(muleContext, transactionConfig, false);
  }

  @Override
  public T execute(ExecutionCallback<T> executionCallback) throws Exception {
    return executionInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
