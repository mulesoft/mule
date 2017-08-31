/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * Returns the value of the {@link ExtensionProperties#CONNECTION_PARAM} variable, which is expected to have been previously set
 * on the supplied {@link ExecutionContext}.
 * <p>
 * Notice that for this to work, the {@link ExecutionContext} has to be an instance of {@link ExecutionContextAdapter}
 *
 * @since 4.0
 */
public class ConnectionArgumentResolver implements ArgumentResolver<Object> {

  /**
   * Returns the connection previously set on the {@code executionContext} under the key
   * {@link ExtensionProperties#CONNECTION_PARAM}
   *
   * @param executionContext an {@link ExecutionContext}
   * @return the connection
   * @throws IllegalArgumentException if the connection was not set
   * @throws ClassCastException if {@code executionContext} is not an {@link ExecutionContextAdapter}
   */
  @Override
  public Object resolve(ExecutionContext executionContext) {
    ConnectionHandler connectionHandler =
        ((ExecutionContextAdapter<ComponentModel>) executionContext).getVariable(CONNECTION_PARAM);
    checkArgument(connectionHandler != null,
                  "No connection was provided for the component [" + executionContext.getComponentModel().getName() + "]");

    try {
      return connectionHandler.getConnection();
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage(String
          .format("Error was found trying to obtain a connection to execute %s '%s' of extension '%s'",
                  getComponentModelTypeName(executionContext.getComponentModel()),
                  executionContext.getComponentModel().getName(),
                  executionContext.getExtensionModel().getName())), e);
    }
  }
}
