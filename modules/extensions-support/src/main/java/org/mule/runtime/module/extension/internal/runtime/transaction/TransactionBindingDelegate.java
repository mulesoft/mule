/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.transaction;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.sdk.api.connectivity.TransactionalConnection;
import org.mule.sdk.api.connectivity.XATransactionalConnection;

import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Binds a connection to a given transaction and returns the {@link ConnectionHandler} that has been bound to it.
 *
 * @since 4.0
 */
public class TransactionBindingDelegate {

  private static final Logger LOGGER = getLogger(TransactionBindingDelegate.class);

  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;

  public TransactionBindingDelegate(ExtensionModel extensionModel, ComponentModel componentModel) {

    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
  }

  /**
   * @param txKey                     the transaction key
   * @param connectionHandlerSupplier {@link Supplier} to get the {@link ConnectionHandler} of the current component
   * @return The {@link ConnectionHandler} that has be bound to the transaction.
   * @throws ConnectionException  if a problem occurred retrieving the {@link ConnectionHandler}
   * @throws TransactionException if the connection could not be bound to the current transaction
   */
  public <T extends TransactionalConnection> ConnectionHandler<T> getBoundResource(boolean lazyConnections,
                                                                                   ExtensionTransactionKey txKey,
                                                                                   ConnectionSupplier<ConnectionHandler<T>> connectionHandlerSupplier)
      throws ConnectionException, TransactionException {

    final Transaction currentTx = TransactionCoordination.getInstance().getTransaction();
    if (currentTx == null) {
      return connectionHandlerSupplier.get();
    }

    if (currentTx.hasResource(txKey)) {
      return new TransactionalConnectionHandler((ExtensionTransactionalResource) currentTx.getResource(txKey));
    } else {
      if (lazyConnections) {
        return new ConnectionHandler<T>() {

          private final LazyValue<ConnectionHandler<T>> boundResource = new LazyValue<>(() -> {
            try {
              return bindResource(txKey, connectionHandlerSupplier, currentTx);
            } catch (ConnectionException e) {
              // Wrap this in a TransactionException to avoid the reconnection/retry mechanism
              throw new MuleRuntimeException(new TransactionException(createStaticMessage("Cannot establish connection for the transaction: "
                  + e.getMessage()), e));
            } catch (TransactionException e) {
              throw new MuleRuntimeException(e);
            }
          });

          @Override
          public T getConnection() throws ConnectionException {
            return boundResource.get().getConnection();
          }

          @Override
          public void release() {
            boundResource.ifComputed(ConnectionHandler::release);
          }

          @Override
          public void invalidate() {
            boundResource.ifComputed(ConnectionHandler::invalidate);
          }
        };
      } else {
        return bindResource(txKey, connectionHandlerSupplier, currentTx);
      }
    }
  }

  private <T extends TransactionalConnection> ConnectionHandler<T> bindResource(ExtensionTransactionKey txKey,
                                                                                ConnectionSupplier<ConnectionHandler<T>> connectionHandlerSupplier,
                                                                                final Transaction currentTx)
      throws ConnectionException, TransactionException {
    ConnectionHandler<T> connectionHandler = connectionHandlerSupplier.get();

    T connection = requireNonNull(connectionHandler
        .getConnection(), () -> format("connection from '%s' (%s '%s' of extension '%s') is null",
                                       connectionHandler,
                                       getComponentModelTypeName(componentModel),
                                       componentModel.getName(),
                                       extensionModel.getName()));
    ExtensionTransactionalResource<T> txResource = createTransactionalResource(currentTx, connectionHandler, connection);
    boolean bound = false;
    try {
      if (currentTx.supports(txKey, txResource)) {
        currentTx.bindResource(txKey, txResource);
        bound = true;
        return new TransactionalConnectionHandler<>(txResource);
      } else {
        throw new TransactionException(createStaticMessage(format("%s '%s' of extension '%s' uses a transactional connection '%s', but the current transaction "
            + "doesn't support it and could not be bound",
                                                                  getComponentModelTypeName(componentModel),
                                                                  componentModel.getName(),
                                                                  extensionModel.getName(),
                                                                  connection.getClass().getName())));
      }
    } catch (Exception e) {
      if (extractConnectionException(e).isPresent()) {
        connectionHandler.invalidate();
      }
      throw e;
    } finally {
      if (!bound) {
        try {
          connectionHandler.release();
        } catch (Exception e) {
          final String msg = "Ignored '" + e.getClass().getName() + ": " + e.getMessage() + "' during connection release";
          if (LOGGER.isDebugEnabled()) {
            LOGGER.warn(msg, e);
          } else {
            LOGGER.warn(msg);
          }
        }
      }
    }
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
