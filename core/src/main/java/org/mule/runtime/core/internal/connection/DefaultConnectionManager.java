/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConnectionManager} which manages connections
 * opened on a specific application.
 *
 * @since 4.0
 */
public final class DefaultConnectionManager implements ConnectionManagerAdapter, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectionManager.class);

    private final Map<ConnectionKey, ConnectionHandlingStrategyAdapter> connections = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final MuleContext muleContext;
    private final RetryPolicyTemplate retryPolicyTemplate;
    private final PoolingProfile poolingProfile;

    /**
     * Creates a new instance
     *
     * @param muleContext the {@link MuleContext} of the owned application
     */
    @Inject
    public DefaultConnectionManager(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.poolingProfile = new PoolingProfile();
        this.retryPolicyTemplate = new NoRetryPolicyTemplate();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if invoked while the {@link #muleContext} is stopped or stopping
     */
    public <Config, Connection> void bind(Config owner, ConnectionProvider<Config, Connection> connectionProvider)
    {
        assertNotStopping(muleContext, "Mule is shutting down... cannot bind new connections");

        connectionProvider = new LifecycleAwareConnectionProviderWrapper<>(connectionProvider, muleContext);

        ConnectionHandlingStrategyAdapter<Config, Connection> managementStrategy = getManagementStrategy(owner, connectionProvider);
        ConnectionHandlingStrategyAdapter<Config, Connection> previous = null;

        writeLock.lock();
        try
        {
            previous = connections.put(new ConnectionKey(owner), managementStrategy);
        }
        finally
        {
            writeLock.unlock();
        }

        if (previous != null)
        {
            close(previous);
        }
    }

    /**
     * {@inheritDoc}
     */
    //TODO: MULE-9082
    @Override
    public void unbind(Object config)
    {
        ConnectionHandlingStrategyAdapter managementStrategy;
        writeLock.lock();
        try
        {
            managementStrategy = connections.remove(new ConnectionKey(config));
        }
        finally
        {
            writeLock.unlock();
        }

        if (managementStrategy != null)
        {
            close(managementStrategy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Config, Connection> ConnectionHandler<Connection> getConnection(Config config) throws ConnectionException
    {
        ConnectionHandlingStrategy<Connection> handlingStrategy = null;
        readLock.lock();
        try
        {
            handlingStrategy = connections.get(new ConnectionKey(config));
        }
        finally
        {
            readLock.unlock();
        }

        if (handlingStrategy == null)
        {
            throw new ConnectionException("No ConnectionProvider has been registered for owner " + config);
        }

        return handlingStrategy.getConnectionHandler();
    }

    /**
     * Breaks all bindings and closes all connections
     *
     * @throws MuleException in case of error.
     */
    @Override
    public void stop() throws MuleException
    {
        writeLock.lock();
        try
        {
            connections.values().stream().forEach(this::close);
            connections.clear();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    //TODO: MULE-9082
    private void close(ConnectionHandlingStrategyAdapter managementStrategy)
    {
        try
        {
            managementStrategy.close();
        }
        catch (Exception e)
        {
            LOGGER.warn("An error was found trying to release connections", e);
        }
    }

    private <Config, Connection> ConnectionHandlingStrategyAdapter<Config, Connection> getManagementStrategy(Config config, ConnectionProvider<Config, Connection> connectionProvider)
    {
        PoolingProfile poolingProfile;
        if (connectionProvider instanceof ConnectionProviderWrapper)
        {
            poolingProfile = ((ConnectionProviderWrapper) connectionProvider).getPoolingProfile().orElse(getDefaultPoolingProfile());
        }
        else
        {
            poolingProfile = getDefaultPoolingProfile();
        }

        ConnectionHandlingStrategyFactory<Config, Connection> connectionHandlingStrategyFactory;
        connectionHandlingStrategyFactory = new DefaultConnectionHandlingStrategyFactory<>(config, connectionProvider, poolingProfile, muleContext);
        return (ConnectionHandlingStrategyAdapter<Config, Connection>) connectionProvider.getHandlingStrategy(connectionHandlingStrategyFactory);
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(retryPolicyTemplate, LOGGER);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(retryPolicyTemplate, true, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(retryPolicyTemplate);
    }

    private class ConnectionKey
    {

        private final Object key;

        private ConnectionKey(Object key)
        {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ConnectionKey)
            {
                return key == ((ConnectionKey) obj).key;
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetryPolicyTemplate getDefaultRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoolingProfile getDefaultPoolingProfile()
    {
        return poolingProfile;
    }

}
