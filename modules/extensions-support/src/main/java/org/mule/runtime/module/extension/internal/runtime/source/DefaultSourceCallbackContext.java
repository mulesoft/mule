/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.tx.TransactionHandle;
import org.mule.runtime.module.extension.internal.runtime.notification.DefaultExtensionNotification;
import org.mule.runtime.module.extension.internal.runtime.transaction.DefaultTransactionHandle;
import org.mule.runtime.module.extension.internal.runtime.transaction.NullTransactionHandle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link SourceCallbackContext}
 *
 * @since 4.0
 */
class DefaultSourceCallbackContext implements SourceCallbackContextAdapter {

  private static final TransactionHandle NULL_TRANSACTION_HANDLE = new NullTransactionHandle();
  private static final TransactionHandle DEFAULT_TRANSACTION_HANDLE = new DefaultTransactionHandle();

  private final SourceCallbackAdapter sourceCallback;
  private final Map<String, Object> variables = new SmallMap<>();
  private String correlationId;
  private Object connection = null;
  private TransactionHandle transactionHandle = NULL_TRANSACTION_HANDLE;
  private boolean dispatched = false;
  private final List<NotificationFunction> notificationFunctions = new LinkedList<>();

  /**
   * Creates a new instance
   *
   * @param sourceCallback the owning {@link SourceCallbackAdapter}
   */
     DefaultSourceCallbackContext(SourceCallbackAdapter sourceCallback) {
    this.sourceCallback = sourceCallback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionHandle bindConnection(Object connection) throws ConnectionException, TransactionException {
    checkArgument(connection != null, "Cannot bind a null connection");
    if (this.connection != null) {
      throw new IllegalStateException("Connection can only be set once per " + SourceCallbackContext.class.getSimpleName());
    }

    this.connection = connection;

    try {
      if (sourceCallback.getTransactionConfig().isTransacted() && connection instanceof TransactionalConnection) {
        ConnectionHandler<Object> connectionHandler = sourceCallback.getSourceConnectionManager().getConnectionHandler(connection)
            .orElseThrow(() -> new TransactionException(createWrongConnectionMessage(connection)));

        sourceCallback.getTransactionSourceBinder().bindToTransaction(sourceCallback.getTransactionConfig(),
                                                                      sourceCallback.getConfigurationInstance(),
                                                                      sourceCallback.getSourceLocation(),
                                                                      connectionHandler, sourceCallback.getTransactionManager(),
                                                                      sourceCallback.getTimeout());
        transactionHandle = DEFAULT_TRANSACTION_HANDLE;
      }
    } catch (Exception e) {
      releaseConnection();
      throw e;
    }

    return transactionHandle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getConnection() {
    if (connection == null) {
      throw new IllegalStateException("No connection has been bound");
    }

    return (T) connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatched() {
    dispatched = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void releaseConnection() {
    if (connection != null) {
      sourceCallback.getSourceConnectionManager().release(connection);
      connection = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionHandle getTransactionHandle() {
    return transactionHandle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasVariable(String variableName) {
    return variables.containsKey(variableName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Optional<T> getVariable(String variableName) {
    return ofNullable((T) variables.get(variableName));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void addVariable(String variableName, Object value) {
    variables.put(variableName, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCorrelationId(String correlationId) {
    checkState(!dispatched, "Cannot set the correlationId at this moment. This context was already used to dispatch a message");
    this.correlationId = correlationId;
  }

  @Override
  public Optional<String> getCorrelationId() {
    return ofNullable(correlationId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> SourceCallback<T, A> getSourceCallback() {
    return sourceCallback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fireOnHandle(NotificationActionDefinition<?> action, TypedValue<?> data) {
    notificationFunctions.add(new ExtensionNotificationFunction() {

      @Override
      public String getActionName() {
        return ((Enum) action).name();
      }

      @Override
      public Notification apply(Event event, Component component) {
        return new DefaultExtensionNotification(event, component, action, data);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NotificationFunction> getNotificationsFunctions() {
    return notificationFunctions;
  }

  private I18nMessage createWrongConnectionMessage(Object connection) {
    return createStaticMessage(format("Internal Error. The transacted source [%s] from the Extension [%s] tried to bind an " +
        "connection of type [%s] which is not a connection created by this extension. ", sourceCallback.getOwningSourceName(),
                                      sourceCallback.getOwningExtensionName(), connection.getClass().getName()));
  }

}
