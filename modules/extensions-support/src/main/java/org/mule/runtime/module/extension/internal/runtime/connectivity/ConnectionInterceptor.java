/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.lang.String.format;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.runtime.RetryRequest;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionalResource;

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
        context.setVariable(CONNECTION_PARAM, getConnection(context));
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
        ConnectionHandler connection = ((OperationContextAdapter) operationContext).removeVariable(CONNECTION_PARAM);
        if (connection != null)
        {
            connection.release();
        }
    }

    /**
     * If the {@code exception} is a {@link ConnectionException} a retry of failed request will be asked
     *
     * @param operationContext the {@link OperationContext} that was used to execute the operation
     * @param retryRequest     a {@link RetryRequest} in case that the operation should be retried
     * @param exception        the {@link Exception} that was thrown by the failing operation
     * @return the same {@link Throwable} given in the parameter
     */
    @Override
    public Throwable onError(OperationContext operationContext, RetryRequest retryRequest, Throwable exception)
    {
        Optional<ConnectionException> connectionException = extractConnectionException(exception);
        if (connectionException.isPresent())
        {
            retryRequest.request();
        }
        return exception;
    }

    private ConnectionHandler<?> getConnection(OperationContextAdapter operationContext) throws ConnectionException, TransactionException
    {
        return operationContext.getTransactionConfig().isPresent()
               ? getTransactedConnectionHandler(operationContext, operationContext.getTransactionConfig().get())
               : getTransactionlessConnectionHandler(operationContext);
    }

    private <T> ConnectionHandler<T> getTransactionlessConnectionHandler(OperationContext operationContext) throws ConnectionException
    {
        Optional<ConnectionProvider> connectionProvider = operationContext.getConfiguration().getConnectionProvider();
        if (!connectionProvider.isPresent())
        {
            throw new IllegalStateException(format("Operation '%s' of extension '%s' requires a connection but was executed with config '%s' which " +
                                                   "is not associated to a connection provider",
                                                   operationContext.getOperationModel().getName(),
                                                   operationContext.getConfiguration().getModel().getExtensionModel().getName(),
                                                   operationContext.getConfiguration().getName()));
        }

        return connectionManager.getConnection(operationContext.getConfiguration().getValue());
    }

    private <T extends TransactionalConnection> ConnectionHandler<T> getTransactedConnectionHandler(OperationContextAdapter operationContext, TransactionConfig transactionConfig) throws ConnectionException, TransactionException
    {
        if (transactionConfig.getAction() == ACTION_NOT_SUPPORTED)
        {
            return getTransactionlessConnectionHandler(operationContext);
        }

        final ExtensionTransactionKey txKey = new ExtensionTransactionKey(operationContext.getConfiguration());
        final Transaction currentTx = TransactionCoordination.getInstance().getTransaction();

        if (currentTx != null)
        {
            if (currentTx.hasResource(txKey))
            {
                return new TransactionalConnectionHandler((ExtensionTransactionalResource) currentTx.getResource(txKey));
            }

            ConnectionHandler<T> connectionHandler = getTransactionlessConnectionHandler(operationContext);
            T connection = connectionHandler.getConnection();
            ExtensionTransactionalResource<T> txResource = new ExtensionTransactionalResource<>(connection, connectionHandler, currentTx);
            boolean bound = false;
            try
            {
                if (currentTx.supports(txKey, txResource))
                {
                    currentTx.bindResource(txKey, txResource);
                    bound = true;
                    return new TransactionalConnectionHandler(txResource);
                }
                else if (transactionConfig.isTransacted())
                {
                    throw new TransactionException(createStaticMessage(format("Operation '%s' of extension '%s' is transactional but current transaction doesn't " +
                                                                              "support connections of type '%s'",
                                                                              operationContext.getOperationModel().getName(),
                                                                              operationContext.getConfiguration().getModel().getExtensionModel().getName(),
                                                                              connectionHandler.getClass().getName())));
                }
            }
            finally
            {
                if (!bound)
                {
                    connectionHandler.release();
                }
            }
        }

        return getTransactionlessConnectionHandler(operationContext);
    }
}
