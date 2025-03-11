/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.transaction.TransactionUtils.profileTransactionAction;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.extension.api.tx.TransactionHandle;
import org.mule.runtime.module.extension.internal.runtime.notification.DefaultExtensionNotification;
import org.mule.runtime.module.extension.internal.runtime.source.trace.SourceDistributedTraceContextManager;
import org.mule.runtime.module.extension.internal.runtime.transaction.DefaultTransactionHandle;
import org.mule.runtime.module.extension.internal.runtime.transaction.NullTransactionHandle;
import org.mule.sdk.api.connectivity.TransactionalConnection;
import org.mule.sdk.api.notification.NotificationActionDefinition;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;
import org.slf4j.Logger;

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
  private static final Logger LOGGER = getLogger(DefaultSourceCallbackContext.class);

  private final SourceCallbackAdapter sourceCallback;
  private final Map<String, Object> variables = new SmallMap<>();
  private String correlationId;
  private Object connection = null;
  private TransactionHandle transactionHandle = NULL_TRANSACTION_HANDLE;
  private boolean dispatched = false;
  private final List<NotificationFunction> notificationFunctions = new LinkedList<>();

  private ProfilingDataProducer<TransactionProfilingEventContext, Object> startProducer;
  private DistributedTraceContextManager sourceDistributedTraceContext = new SourceDistributedTraceContextManager();
  private final ProfilingService profilingService;

  private final boolean errorAfterTimeout;

  /**
   * Creates a new instance
   *
   * @param sourceCallback the owning {@link SourceCallbackAdapter}
   */
  DefaultSourceCallbackContext(SourceCallbackAdapter sourceCallback, ProfilingService profilingService,
                               boolean errorAfterTimeout) {
    this.sourceCallback = sourceCallback;
    this.profilingService = profilingService;
    this.errorAfterTimeout = errorAfterTimeout;
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
                                                                      connectionHandler,
                                                                      sourceCallback.getTimeout(), errorAfterTimeout);
        if (sourceCallback.getTransactionConfig().isTransacted()) {
          initialiseProfilingDataProducerIfNeeded();
          profileTransactionAction(startProducer, TX_START, sourceCallback.getSourceLocation());
        }
        transactionHandle = DEFAULT_TRANSACTION_HANDLE;
      }
    } catch (Exception e) {
      LOGGER.warn("Connection could not be bound", e);
      releaseConnection();
      throw e;
    }

    return transactionHandle;
  }

  private void initialiseProfilingDataProducerIfNeeded() {
    if (startProducer == null) {
      startProducer = profilingService.getProfilingDataProducer(TX_START);
    }
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

  @Override
  public DistributedTraceContextManager getDistributedSourceTraceContext() {
    return sourceDistributedTraceContext;
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
