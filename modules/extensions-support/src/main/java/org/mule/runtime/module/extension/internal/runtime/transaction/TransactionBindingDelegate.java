/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;

import java.util.function.Supplier;

/**
 * Binds a connection to a given transaction and returns the {@link ConnectionHandler} that has been bound to it.
 *
 * @since 4.0
 */
public class TransactionBindingDelegate {

  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;

  public TransactionBindingDelegate(ExtensionModel extensionModel, ComponentModel componentModel) {

    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
  }

  /**
   * @param transactionConfig given transaction config
   * @param txKey the transaction key
   * @param connectionHandlerSupplier {@link Supplier} to get the {@link ConnectionHandler} of the current component
   * @return The {@link ConnectionHandler} that has be bound to the transaction.
   * @throws ConnectionException if a problem occurred retrieving the {@link ConnectionHandler}
   * @throws TransactionException if the connection could not be bound to the current transaction
   */
  public <T extends TransactionalConnection> ConnectionHandler<T> getBoundResource(TransactionConfig transactionConfig,
                                                                                   ExtensionTransactionKey txKey,
                                                                                   ConnectionSupplier<ConnectionHandler<T>> connectionHandlerSupplier)
      throws ConnectionException, TransactionException {

    final Transaction currentTx = TransactionCoordination.getInstance().getTransaction();
    if (currentTx != null) {

      if (currentTx.hasResource(txKey)) {
        return new TransactionalConnectionHandler((ExtensionTransactionalResource) currentTx.getResource(txKey));
      }

      ConnectionHandler<T> connectionHandler = connectionHandlerSupplier.get();
      T connection = connectionHandler.getConnection();

      ExtensionTransactionalResource<T> txResource = createTransactionalResource(currentTx, connectionHandler, connection);
      boolean bound = false;
      try {
        if (currentTx.supports(txKey, txResource)) {
          currentTx.bindResource(txKey, txResource);
          bound = true;
          return new TransactionalConnectionHandler(txResource);
        } else if (transactionConfig.isTransacted()) {
          throw new TransactionException(createStaticMessage(format("%s '%s' of extension '%s' uses a transactional connection '%s', but the current transaction "
              + "doesn't support it and could not be bound",
                                                                    getComponentModelTypeName(componentModel),
                                                                    componentModel.getName(),
                                                                    extensionModel.getName(),
                                                                    connection.getClass().getName())));
        }
      } finally {
        if (!bound) {
          connectionHandler.release();
        }
      }
    }

    return connectionHandlerSupplier.get();
  }

  private ExtensionTransactionalResource createTransactionalResource(Transaction currentTx, ConnectionHandler connectionHandler,
                                                                     Object connection) {
    return connection instanceof XATransactionalConnection
        ? new XAExtensionTransactionalResource((XATransactionalConnection) connection, connectionHandler, currentTx)
        : new ExtensionTransactionalResource((TransactionalConnection) connection, connectionHandler, currentTx);
  }

  @FunctionalInterface
  public interface ConnectionSupplier<T> {

    T get() throws ConnectionException;
  }
}
