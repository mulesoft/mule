/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static org.mule.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ManagedConnection;
import org.mule.api.connector.ConnectionManager;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

import java.util.Optional;

import javax.inject.Inject;

/**
 * Implements simple connection management by using the {@link #before(OperationContext)} phase to
 * set a connection as parameter value of key {@link ExtensionProperties#CONNECTION_PARAM} into an
 * {@link OperationContext}.
 *
 * @since 4.0
 */
public final class ConnectionInterceptor implements Interceptor
{

    @Inject
    private ConnectionManager connectionManager;

    /**
     * Adds a {@code Connection} as a parameter in the {@code operationContext}, following the
     * considerations in this type's javadoc.
     *
     * @param operationContext the {@link OperationContext} for the operation to be executed
     * @throws IllegalArgumentException if the {@code operationContext} already contains a parameter of key {@link ExtensionProperties#CONNECTION_PARAM}
     */
    @Override
    public void before(OperationContext operationContext) throws Exception
    {
        OperationContextAdapter context = (OperationContextAdapter) operationContext;
        checkArgument(context.getVariable(CONNECTION_PARAM) == null, "A connection was already set for this operation context");
        context.setVariable(CONNECTION_PARAM, getConnection(operationContext));
    }

    /**
     * Sets the {@link ExtensionProperties#CONNECTION_PARAM} parameter on the {@code operationContext} to {@code null}
     *
     * @param operationContext the {@link OperationContext} that was used to execute the operation
     * @param result           the operation's result
     */
    @Override
    public void after(OperationContext operationContext, Object result)
    {
        ManagedConnection connection = ((OperationContextAdapter) operationContext).removeVariable(CONNECTION_PARAM);
        if (connection != null)
        {
            connection.release();
        }
    }

    //TODO: MULE-8909 && MULE-8910: validate the connection before returning it. Reconnect if necessary
    private Object getConnection(OperationContext operationContext) throws ConnectionException
    {
        Optional<ConnectionProvider> connectionProvider = operationContext.getConfiguration().getConnectionProvider();
        if (!connectionProvider.isPresent())
        {
            throw new IllegalStateException(String.format("Operation '%s' of extension '%s' requires a connection but was executed with config '%s' which " +
                                                          "is not associated to a connection provider",
                                                          operationContext.getOperationModel().getName(),
                                                          operationContext.getConfiguration().getModel().getExtensionModel().getName(),
                                                          operationContext.getConfiguration().getName()));
        }

        return connectionManager.getConnection(operationContext.getConfiguration().getValue());
    }
}
