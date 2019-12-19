/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.api.util.StreamingUtils.supportsStreaming;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

import java.util.function.Consumer;

/**
 * Implements simple connection management by using the {@link #before(ExecutionContext)} phase to set a connection as parameter
 * value of key {@link ExtensionProperties#CONNECTION_PARAM} into an {@link ExecutionContext}.
 *
 * @since 4.0
 */
public final class ConnectionInterceptor implements Interceptor<ComponentModel> {

  private static final String CLOSE_CONNECTION_COMMAND = "closeCommand";

  private final ExtensionConnectionSupplier connectionSupplier;

  public ConnectionInterceptor(ExtensionConnectionSupplier connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  /**
   * Adds a {@code Connection} as a parameter in the {@code operationContext}, following the considerations in this type's
   * javadoc.
   *
   * @param executionContext the {@link ExecutionContext} for the operation to be executed
   * @throws IllegalArgumentException if the {@code operationContext} already contains a parameter of key
   *                                  {@link ExtensionProperties#CONNECTION_PARAM}
   */
  @Override
  public void before(ExecutionContext<ComponentModel> executionContext) throws Exception {
    final ComponentModel componentModel = executionContext.getComponentModel();
    if (componentModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
      return;
    }

    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter) executionContext;
    if (context.getVariable(CONNECTION_PARAM) == null) {
      context.setVariable(CONNECTION_PARAM, getConnection(context));

      if (!supportsStreaming(componentModel)) {
        setCloseCommand(executionContext, () -> release(executionContext));
      }
    }
  }

  @Override
  public Throwable onError(ExecutionContext<ComponentModel> executionContext, Throwable exception) {
    if (extractConnectionException(exception).isPresent()) {
      setCloseCommand(executionContext, () -> withConnection(executionContext, ConnectionHandler::invalidate));
    } else {
      setCloseCommand(executionContext, () -> release(executionContext));
    }

    return exception;
  }

  /**
   * Closes the connection according to the command set through {@link #setCloseCommand(ExecutionContext, Runnable)}.
   * Interception API requires the connection to be closed at this point so that it's available across the entire
   * interception cycle.
   */
  @Override
  public void after(ExecutionContext<ComponentModel> executionContext, Object result) {
    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter) executionContext;

    Runnable closeCommand = context.removeVariable(CLOSE_CONNECTION_COMMAND);
    if (closeCommand != null) {
      closeCommand.run();
    }
  }

  private void release(ExecutionContext<ComponentModel> executionContext) {
    withConnection(executionContext, ConnectionHandler::release);
  }

  private void withConnection(ExecutionContext<ComponentModel> executionContext, Consumer<ConnectionHandler> consumer) {
    ConnectionHandler handler = ((ExecutionContextAdapter<ComponentModel>) executionContext).removeVariable(CONNECTION_PARAM);
    if (handler != null) {
      consumer.accept(handler);
    }
  }

  private void setCloseCommand(ExecutionContext<ComponentModel> executionContext, Runnable command) {
    ExecutionContextAdapter<ComponentModel> context = (ExecutionContextAdapter) executionContext;
    context.setVariable(CLOSE_CONNECTION_COMMAND, command);
  }

  private ConnectionHandler<?> getConnection(ExecutionContextAdapter<? extends ComponentModel> operationContext)
      throws ConnectionException, TransactionException {
    return connectionSupplier.getConnection(operationContext);
  }
}
