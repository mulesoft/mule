/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

/**
 * Returns the value of the {@link ExtensionProperties#CONNECTION_PARAM} variable, which is expected to have been previously set
 * on the supplied {@link OperationContext}.
 * <p>
 * Notice that for this to work, the {@link OperationContext} has to be an instance of {@link OperationContextAdapter}
 *
 * @since 4.0
 */
public class ConnectionArgumentResolver implements ArgumentResolver<Object> {

  /**
   * Returns the connection previously set on the {@code operationContext} under the key
   * {@link ExtensionProperties#CONNECTION_PARAM}
   *
   * @param operationContext an {@link OperationContext}
   * @return the connection
   * @throws IllegalArgumentException if the connection was not set
   * @throws ClassCastException if {@code operationContext} is not an {@link OperationContextAdapter}
   */
  @Override
  public Object resolve(OperationContext operationContext) {
    ConnectionHandler connectionHandler = ((OperationContextAdapter) operationContext).getVariable(CONNECTION_PARAM);
    checkArgument(connectionHandler != null, "No connection was provided for the operation");

    try {
      return connectionHandler.getConnection();
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(MessageFactory.createStaticMessage(String
          .format("Error was found trying to obtain a connection to execute operation '%s' of extension '%s'",
                  operationContext.getOperationModel().getName(),
                  operationContext.getConfiguration().getModel().getExtensionModel().getName())), e);
    }
  }
}
