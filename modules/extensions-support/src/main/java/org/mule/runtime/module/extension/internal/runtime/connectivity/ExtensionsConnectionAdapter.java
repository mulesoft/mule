/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.lang.String.format;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NONE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionBindDelegate;

import javax.inject.Inject;
import java.util.Optional;

/**
 * A bridge between the execution of a {@link ComponentModel} and the {@link ConnectionManager} which provides
 * the connections that it needs.
 * <p>
 * It handles connection provisioning and transaction support
 *
 * @since 4.0
 */
public class ExtensionsConnectionAdapter {

  @Inject
  private ConnectionManager connectionManager;

  private final TransactionBindDelegate transactionBindDelegate = new TransactionBindDelegate();

  /**
   * Returns the connection to be used with the {@code operationContext}.
   * <p>
   * It accounts for the possibility of the returned connection joining/belonging to an active transaction
   *
   * @param executionContext an {@link ExecutionContextAdapter}
   * @return a {@link ConnectionHandler}
   * @throws ConnectionException  if connection could not be obtained
   * @throws TransactionException if something is wrong with the transaction
   */
  public ConnectionHandler<?> getConnection(ExecutionContextAdapter<? extends ComponentModel> executionContext)
      throws ConnectionException, TransactionException {
    return executionContext.getTransactionConfig().isPresent()
        ? getTransactedConnectionHandler(executionContext, executionContext.getTransactionConfig().get())
        : getTransactionlessConnectionHandler(executionContext);
  }

  private <T extends TransactionalConnection> ConnectionHandler<T> getTransactedConnectionHandler(
                                                                                                  ExecutionContextAdapter<? extends ComponentModel> executionContext,
                                                                                                  TransactionConfig transactionConfig)
      throws ConnectionException, TransactionException {

    if (transactionConfig.getAction() == ACTION_NOT_SUPPORTED || transactionConfig.getAction() == ACTION_NONE) {
      return getTransactionlessConnectionHandler(executionContext);
    }

    ConfigurationInstance configuration = executionContext.getConfiguration()
        .orElseThrow(() -> new IllegalStateException(format(
                                                            "Operation '%s' of extension '%s' cannot participate in a transaction because it doesn't have a config",
                                                            executionContext.getComponentModel().getName(),
                                                            executionContext.getExtensionModel().getName())));


    final ExtensionTransactionKey txKey = new ExtensionTransactionKey(configuration);
    final Transaction currentTx = TransactionCoordination.getInstance().getTransaction();

    return transactionBindDelegate.bindResource(executionContext.getComponentModel(), executionContext.getExtensionModel(),
                                                transactionConfig, txKey,
                                                currentTx, () -> getTransactionlessConnectionHandler(executionContext));
  }

  private <T> ConnectionHandler<T> getTransactionlessConnectionHandler(ExecutionContext executionContext)
      throws ConnectionException {


    final Optional<ConfigurationInstance> configuration = executionContext.getConfiguration();
    Optional<ConnectionProvider> connectionProvider = configuration
        .map(ConfigurationInstance::getConnectionProvider)
        .map(Optional::get);

    if (!connectionProvider.isPresent()) {
      String configRef = configuration
          .map(config -> format("with config '%s' which is not associated to a connection provider", config.getName()))
          .orElse("without a config");

      throw new IllegalStateException(format("%s '%s' of extension '%s' requires a connection but was executed %s",
                                             getComponentModelTypeName(executionContext.getComponentModel()),
                                             executionContext.getComponentModel().getName(),
                                             executionContext.getExtensionModel().getName(),
                                             configRef));
    }

    return connectionManager.getConnection(configuration.get().getValue());
  }
}
