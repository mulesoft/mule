/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

import javax.inject.Inject;

/**
 * Implements simple connection management by using the {@link #before(ExecutionContext)} phase to set a connection as parameter
 * value of key {@link ExtensionProperties#CONNECTION_PARAM} into an {@link ExecutionContext}.
 *
 * @since 4.0
 */
public final class ConnectionInterceptor implements Interceptor {

  @Inject
  private ExtensionConnectionSupplier connectionSupplier;

  /**
   * Adds a {@code Connection} as a parameter in the {@code operationContext}, following the considerations in this type's
   * javadoc.
   *
   * @param executionContext the {@link ExecutionContext} for the operation to be executed
   * @throws IllegalArgumentException if the {@code operationContext} already contains a parameter of key
   *         {@link ExtensionProperties#CONNECTION_PARAM}
   */
  @Override
  public void before(ExecutionContext<OperationModel> executionContext) throws Exception {
    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter) executionContext;
    checkArgument(context.getVariable(CONNECTION_PARAM) == null, "A connection was already set for this operation context");
    context.setVariable(CONNECTION_PARAM, getConnection(context));
  }

  /**
   * Sets the {@link ExtensionProperties#CONNECTION_PARAM} parameter on the {@code operationContext} to {@code null}
   *
   * @param executionContext the {@link ExecutionContext} that was used to execute the operation
   * @param result the operation's result
   */
  @Override
  public void after(ExecutionContext<OperationModel> executionContext, Object result) {
    ConnectionHandler connection = ((ExecutionContextAdapter<OperationModel>) executionContext).removeVariable(CONNECTION_PARAM);
    if (connection != null) {
      connection.release();
    }
  }

  private ConnectionHandler<?> getConnection(ExecutionContextAdapter<? extends ComponentModel> operationContext)
      throws ConnectionException, TransactionException {
    return connectionSupplier.getConnection(operationContext);
  }
}
