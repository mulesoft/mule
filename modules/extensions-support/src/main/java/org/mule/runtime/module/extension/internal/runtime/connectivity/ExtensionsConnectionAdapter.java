/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.lang.String.format;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionalResource;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionalConnectionHandler;
import org.mule.runtime.module.extension.internal.runtime.transaction.XAExtensionTransactionalResource;

import java.util.Optional;

import javax.inject.Inject;

/**
 * A bridge between the execution of an operation and the {@link ConnectionManager} which provides the connections that it needs.
 * <p>
 * It handles connection provisioning and transaction support
 *
 * @since 4.0
 */
public class ExtensionsConnectionAdapter {

  @Inject
  private ConnectionManager connectionManager;

  /**
   * Returns the connection to be used with the {@code operationContext}.
   * <p>
   * It accounts for the possibility of the returned connection joining/belonging to an active transaction
   *
   * @param operationContext an {@link OperationContextAdapter}
   * @return a {@link ConnectionHandler}
   * @throws ConnectionException if connection could not be obtained
   * @throws TransactionException if something is wrong with the transaction
   */
  public ConnectionHandler<?> getConnection(OperationContextAdapter operationContext)
      throws ConnectionException, TransactionException {
    return operationContext.getTransactionConfig().isPresent()
        ? getTransactedConnectionHandler(operationContext, operationContext.getTransactionConfig().get())
        : getTransactionlessConnectionHandler(operationContext);
  }


  private <T> ConnectionHandler<T> getTransactionlessConnectionHandler(OperationContext operationContext)
      throws ConnectionException {
    Optional<ConnectionProvider> connectionProvider = operationContext.getConfiguration().getConnectionProvider();
    if (!connectionProvider.isPresent()) {
      throw new IllegalStateException(format("Operation '%s' of extension '%s' requires a connection but was executed with config '%s' which "
          + "is not associated to a connection provider", operationContext.getOperationModel().getName(),
                                             operationContext.getConfiguration().getModel().getExtensionModel().getName(),
                                             operationContext.getConfiguration().getName()));
    }

    return connectionManager.getConnection(operationContext.getConfiguration().getValue());
  }

  private <T extends TransactionalConnection> ConnectionHandler<T> getTransactedConnectionHandler(OperationContextAdapter operationContext,
                                                                                                  TransactionConfig transactionConfig)
      throws ConnectionException, TransactionException {
    if (transactionConfig.getAction() == ACTION_NOT_SUPPORTED) {
      return getTransactionlessConnectionHandler(operationContext);
    }

    final ExtensionTransactionKey txKey = new ExtensionTransactionKey(operationContext.getConfiguration());
    final Transaction currentTx = TransactionCoordination.getInstance().getTransaction();

    if (currentTx != null) {
      if (currentTx.hasResource(txKey)) {
        return new TransactionalConnectionHandler((ExtensionTransactionalResource) currentTx.getResource(txKey));
      }

      ConnectionHandler<T> connectionHandler = getTransactionlessConnectionHandler(operationContext);
      T connection = connectionHandler.getConnection();
      ExtensionTransactionalResource<T> txResource = createTransactionalResource(currentTx, connectionHandler, connection);
      boolean bound = false;
      try {
        if (currentTx.supports(txKey, txResource)) {
          currentTx.bindResource(txKey, txResource);
          bound = true;
          return new TransactionalConnectionHandler(txResource);
        } else if (transactionConfig.isTransacted()) {
          throw new TransactionException(createStaticMessage(format("Operation '%s' of extension '%s' is transactional but current transaction doesn't "
              + "support connections of type '%s'", operationContext.getOperationModel().getName(),
                                                                    operationContext.getConfiguration().getModel()
                                                                        .getExtensionModel().getName(),
                                                                    connectionHandler.getClass().getName())));
        }
      } finally {
        if (!bound) {
          connectionHandler.release();
        }
      }
    }

    return getTransactionlessConnectionHandler(operationContext);
  }

  private <T extends TransactionalConnection> ExtensionTransactionalResource<T> createTransactionalResource(Transaction currentTx,
                                                                                                            ConnectionHandler<T> connectionHandler,
                                                                                                            T connection) {
    return connection instanceof XATransactionalConnection
        ? new XAExtensionTransactionalResource((XATransactionalConnection) connection, connectionHandler, currentTx)
        : new ExtensionTransactionalResource<>(connection, connectionHandler, currentTx);
  }
}
