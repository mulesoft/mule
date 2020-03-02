/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.execution.BeginAndResolveTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ExecuteCallbackInterceptor;
import org.mule.runtime.core.internal.execution.ExecutionContext;
import org.mule.runtime.core.internal.execution.ExecutionInterceptor;
import org.mule.runtime.core.internal.execution.IsolateCurrentTransactionInterceptor;
import org.mule.runtime.core.internal.execution.SuspendXaTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ValidateTransactionalStateInterceptor;
import org.mule.runtime.core.internal.execution.compatibility.ResolvePreviousTransactionInterceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import javax.transaction.TransactionManager;

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
public final class TransactionalExecutionTemplate<T> implements ExecutionTemplate<T> {

  private ExecutionInterceptor<T> executionInterceptor;

  private TransactionalExecutionTemplate(String applicationName, NotificationDispatcher notificationDispatcher,
                                         SingleResourceTransactionFactoryManager transactionFactoryManager,
                                         TransactionManager transactionManager, TransactionConfig transactionConfig) {
    this(applicationName, notificationDispatcher, transactionFactoryManager, transactionManager, transactionConfig, true, false);
  }

  private TransactionalExecutionTemplate(String applicationName, NotificationDispatcher notificationDispatcher,
                                         SingleResourceTransactionFactoryManager transactionFactoryManager,
                                         TransactionManager transactionManager, TransactionConfig transactionConfig,
                                         boolean resolveAnyTransaction, boolean resolvePreviousTx) {
    if (transactionConfig == null) {
      transactionConfig = new MuleTransactionConfig();
    }
    final boolean processTransactionOnException = true;
    ExecutionInterceptor<T> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor =
        new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig, applicationName,
                                                    notificationDispatcher, transactionFactoryManager,
                                                    transactionManager,
                                                    processTransactionOnException, resolveAnyTransaction);
    if (resolvePreviousTx) {
      tempExecutionInterceptor = new ResolvePreviousTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
    }
    tempExecutionInterceptor = new SuspendXaTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig,
                                                                     processTransactionOnException);
    tempExecutionInterceptor =
        new ValidateTransactionalStateInterceptor<>(tempExecutionInterceptor, transactionConfig, resolvePreviousTx);
    this.executionInterceptor = new IsolateCurrentTransactionInterceptor<>(tempExecutionInterceptor, transactionConfig);
  }

  /**
   * Creates a ExecutionTemplate that will manage transactional context according to configured TransactionConfig
   *
   * @param muleContext MuleContext for this application
   * @param transactionConfig transaction config for the execution context
   */
  public static <T> TransactionalExecutionTemplate<T> createTransactionalExecutionTemplate(MuleContext muleContext,
                                                                                           TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleContext),
                                                getNotificationDispatcher((MuleContextWithRegistry) muleContext),
                                                muleContext.getTransactionFactoryManager(), muleContext.getTransactionManager(),
                                                transactionConfig);
  }

  /**
   * Creates a ExecutionTemplate that will manage transactional context according to configured TransactionConfig.
   * This is a template which maintains the TX logic for compatibility components.
   *
   * @param muleContext MuleContext for this application
   * @param transactionConfig transaction config for the execution context
   */
  public static <T> TransactionalExecutionTemplate<T> createCompatibilityExecutionTemplate(MuleContext muleContext,
                                                                                           TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleContext),
                                                getNotificationDispatcher((MuleContextWithRegistry) muleContext),
                                                muleContext.getTransactionFactoryManager(),
                                                muleContext.getTransactionManager(),
                                                transactionConfig, true, true);
  }

  private static NotificationDispatcher getNotificationDispatcher(MuleContextWithRegistry muleContext) {
    try {
      return muleContext.getRegistry().lookupObject(NotificationDispatcher.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static String getApplicationName(MuleContext muleContext) {
    return muleContext.getConfiguration().getId();
  }

  /**
   * Creates a TransactionalExecutionTemplate for inner scopes within a flow
   *
   * @param muleContext
   * @param transactionConfig
   * @return
   */
  public static <T> TransactionalExecutionTemplate<T> createScopeTransactionalExecutionTemplate(MuleContext muleContext,
                                                                                                TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleContext),
                                                getNotificationDispatcher((MuleContextWithRegistry) muleContext),
                                                muleContext.getTransactionFactoryManager(),
                                                muleContext.getTransactionManager(),
                                                transactionConfig, false, false);
  }

  @Override
  public T execute(ExecutionCallback<T> executionCallback) throws Exception {
    return executionInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
