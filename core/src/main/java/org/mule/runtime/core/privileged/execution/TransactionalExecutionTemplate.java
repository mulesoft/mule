/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.execution.BeginAndResolveTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ExecuteCallbackInterceptor;
import org.mule.runtime.core.internal.execution.ExecutionContext;
import org.mule.runtime.core.internal.execution.ExecutionInterceptor;
import org.mule.runtime.core.internal.execution.IsolateCurrentTransactionInterceptor;
import org.mule.runtime.core.internal.execution.SuspendXaTransactionInterceptor;
import org.mule.runtime.core.internal.execution.ValidateTransactionalStateInterceptor;
import org.mule.runtime.core.internal.execution.compatibility.ResolvePreviousTransactionInterceptor;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.runtime.core.privileged.transaction.TransactionFactory;

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
                                         TransactionManager transactionManager, TransactionConfig transactionConfig) {
    this(applicationName, notificationDispatcher, transactionManager, transactionConfig, true, false, true);
  }

  private TransactionalExecutionTemplate(String applicationName, NotificationDispatcher notificationDispatcher,
                                         TransactionManager transactionManager, TransactionConfig transactionConfig,
                                         boolean resolveAnyTransaction, boolean resolvePreviousTx, boolean errorAtTimeout) {
    if (transactionConfig == null) {
      transactionConfig = new MuleTransactionConfig();
    }
    final boolean processTransactionOnException = true;
    ExecutionInterceptor<T> tempExecutionInterceptor = new ExecuteCallbackInterceptor<>();
    tempExecutionInterceptor = new BeginAndResolveTransactionInterceptor<>(tempExecutionInterceptor,
                                                                           transactionConfig,
                                                                           applicationName,
                                                                           notificationDispatcher,
                                                                           transactionManager,
                                                                           processTransactionOnException,
                                                                           resolveAnyTransaction, errorAtTimeout);
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
   * Creates a ExecutionTemplate that will manage transactional context according to the given txAction
   */
  public static <T> TransactionalExecutionTemplate<T> createTransactionalExecutionTemplate(Registry registry,
                                                                                           byte txAction,
                                                                                           TransactionFactory factory) {
    final var muleContext = registry.lookupByType(MuleContext.class).orElseThrow();

    MuleTransactionConfig transactionConfig = new MuleTransactionConfig(txAction);
    transactionConfig.setFactory(factory);

    return new TransactionalExecutionTemplate<>(getApplicationName(muleContext),
                                                getNotificationDispatcher((MuleContextWithRegistry) muleContext),
                                                registry.lookupByType(TransactionManager.class).orElse(null),
                                                transactionConfig);
  }

  /**
   * Creates a ExecutionTemplate that will manage transactional context according to configured TransactionConfig
   */
  public static <T> TransactionalExecutionTemplate<T> createTransactionalExecutionTemplate(MuleConfiguration muleConfiguration,
                                                                                           NotificationDispatcher notificationDispatcher,
                                                                                           TransactionManager transactionManager,
                                                                                           TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleConfiguration),
                                                notificationDispatcher,
                                                transactionManager,
                                                transactionConfig);
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

  private static String getApplicationName(MuleConfiguration muleConfiguration) {
    return muleConfiguration.getId();
  }

  /**
   * Creates a TransactionalExecutionTemplate for inner scopes within a flow
   *
   * @return <T>
   */
  public static <T> TransactionalExecutionTemplate<T> createScopeTransactionalExecutionTemplate(MuleConfiguration muleConfiguration,
                                                                                                NotificationDispatcher notificationDispatcher,
                                                                                                TransactionManager transactionManager,
                                                                                                TransactionConfig transactionConfig) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleConfiguration),
                                                notificationDispatcher,
                                                transactionManager,
                                                transactionConfig, false, false, true);
  }

  /**
   * Creates a TransactionalExecutionTemplate for inner scopes within a flow
   *
   * @return <T>
   */
  public static <T> TransactionalExecutionTemplate<T> createScopeTransactionalExecutionTemplate(MuleConfiguration muleConfiguration,
                                                                                                NotificationDispatcher notificationDispatcher,
                                                                                                TransactionManager transactionManager,
                                                                                                TransactionConfig transactionConfig,
                                                                                                boolean errorAfterTimeout) {
    return new TransactionalExecutionTemplate<>(getApplicationName(muleConfiguration),
                                                notificationDispatcher,
                                                transactionManager,
                                                transactionConfig, false, false, errorAfterTimeout);
  }

  @Override
  public T execute(ExecutionCallback<T> executionCallback) throws Exception {
    return executionInterceptor.execute(executionCallback, new ExecutionContext());
  }
}
