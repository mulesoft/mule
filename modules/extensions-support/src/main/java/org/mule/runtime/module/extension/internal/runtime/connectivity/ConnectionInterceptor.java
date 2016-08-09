/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static org.mule.runtime.core.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.extension.api.runtime.RetryRequest;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

import java.util.Optional;

import javax.inject.Inject;

/**
 * Implements simple connection management by using the {@link #before(OperationContext)} phase to set a connection as parameter
 * value of key {@link ExtensionProperties#CONNECTION_PARAM} into an {@link OperationContext}.
 *
 * @since 4.0
 */
public final class ConnectionInterceptor implements Interceptor {

  @Inject
  private ExtensionsConnectionAdapter connectionAdapter;

  /**
   * Adds a {@code Connection} as a parameter in the {@code operationContext}, following the considerations in this type's
   * javadoc.
   *
   * @param operationContext the {@link OperationContext} for the operation to be executed
   * @throws IllegalArgumentException if the {@code operationContext} already contains a parameter of key
   *         {@link ExtensionProperties#CONNECTION_PARAM}
   */
  @Override
  public void before(OperationContext operationContext) throws Exception {
    OperationContextAdapter context = (OperationContextAdapter) operationContext;
    checkArgument(context.getVariable(CONNECTION_PARAM) == null, "A connection was already set for this operation context");
    context.setVariable(CONNECTION_PARAM, getConnection(context));
  }

  /**
   * Sets the {@link ExtensionProperties#CONNECTION_PARAM} parameter on the {@code operationContext} to {@code null}
   *
   * @param operationContext the {@link OperationContext} that was used to execute the operation
   * @param result the operation's result
   */
  @Override
  public void after(OperationContext operationContext, Object result) {
    ConnectionHandler connection = ((OperationContextAdapter) operationContext).removeVariable(CONNECTION_PARAM);
    if (connection != null) {
      connection.release();
    }
  }

  /**
   * If the {@code exception} is a {@link ConnectionException} a retry of failed request will be asked
   *
   * @param operationContext the {@link OperationContext} that was used to execute the operation
   * @param retryRequest a {@link RetryRequest} in case that the operation should be retried
   * @param exception the {@link Exception} that was thrown by the failing operation
   * @return the same {@link Throwable} given in the parameter
   */
  @Override
  public Throwable onError(OperationContext operationContext, RetryRequest retryRequest, Throwable exception) {
    Optional<ConnectionException> connectionException = extractConnectionException(exception);
    if (connectionException.isPresent()) {
      retryRequest.request();
    }
    return exception;
  }

  private ConnectionHandler<?> getConnection(OperationContextAdapter operationContext)
      throws ConnectionException, TransactionException {
    return connectionAdapter.getConnection(operationContext);
  }
}
