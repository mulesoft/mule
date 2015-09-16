/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extension.connection.ConnectionHandler;
import org.mule.extension.runtime.Interceptor;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements simple connection management by using the {@link #before(OperationContext)} phase to
 * set a connection as parameter value of key {@link ExtensionProperties#CONNECTION_PARAM} into an
 * {@link OperationContext}.
 * <p/>
 * The connection is not actually established until the first invocation to the {@link #before(OperationContext)}
 * method and is kept until {@link #stop()} is invoked. This happens into a thread-safe manner so concurrent
 * invocations to {@link #before(OperationContext)} will result on only one connection being established and returned.
 *
 * @param <Config>     the generic type for the Config object that will be used to create the connection
 * @param <Connection> the generic type of the connections to be established
 * @since 4.0
 */
//TODO: Much of the logic here should be moved to the ConnectionService when MULE-8952 is implemented
class ConnectionInterceptor<Config, Connection> implements Interceptor, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionInterceptor.class);

    private final ConnectionHandler<Config, Connection> connectionHandler;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private Connection connection = null;

    @Inject
    private MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param connectionHandler the {@link ConnectionHandler} to be used for connecting/disconnecting
     */
    public ConnectionInterceptor(ConnectionHandler<Config, Connection> connectionHandler)
    {
        checkArgument(connectionHandler != null, "connectionHandler cannot be null");
        this.connectionHandler = connectionHandler;
    }

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
        ((OperationContextAdapter) operationContext).removeVariable(CONNECTION_PARAM);
    }

    //TODO: MULE-8909 && MULE-8910: validate the connection before returning it. Reconnect if necessary
    private Connection getConnection(OperationContext operationContext)
    {
        readLock.lock();
        try
        {
            if (connection != null)
            {
                return connection;
            }
        }
        finally
        {
            readLock.unlock();
        }

        writeLock.lock();
        try
        {
            //check another thread didn't beat us to it
            if (connection != null)
            {
                return connection;
            }

            if (muleContext.isStopped() || muleContext.isStopping())
            {
                throw new IllegalStateException("Mule is shutting down... cannot create a new connection");
            }
            return connection = connectionHandler.connect((Config) operationContext.getConfiguration().getValue());
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Performs dependency injection on the {@link #connectionHandler} and if needed
     * propagates this lifecycle phase into it
     *
     * @throws InitialisationException if anything went wrong
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            muleContext.getInjector().inject(connectionHandler);
            initialiseIfNeeded(connectionHandler, muleContext);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Propagates this lifecycle phase into the {@link #connectionHandler}
     *
     * @throws MuleException if anything went wrong
     */
    @Override
    public void start() throws MuleException
    {
        startIfNeeded(connectionHandler);
    }

    /**
     * If a connection is currently active, then it disconnects it using
     * {@link ConnectionHandler#disconnect(Object)}.
     * <p/>
     * It also guarantees this lifecycle phase being propagated into the {@link #connectionHandler}
     *
     * @throws MuleException if anything went wrong
     */
    @Override
    public void stop() throws MuleException
    {
        boolean handlerStopped = false;
        writeLock.lock();
        try
        {
            if (connection != null)
            {
                connectionHandler.disconnect(connection);
            }
        }
        catch (Exception e)
        {
            try
            {
                stopHandlerSilently();
            }
            finally
            {
                handlerStopped = true;
            }
            throw e;
        }
        finally
        {
            connection = null;
            try
            {
                if (!handlerStopped)
                {
                    stopHandler();
                }
            }
            finally
            {
                writeLock.unlock();
            }
        }
    }

    private void stopHandler() throws MuleException
    {
        stopIfNeeded(connectionHandler);
    }

    private void stopHandlerSilently()
    {
        try
        {
            stopHandler();
        }
        catch (Exception e)
        {
            LOGGER.error("Could not stop connection handler " + connectionHandler, e);
        }
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(connectionHandler, LOGGER);
    }
}
