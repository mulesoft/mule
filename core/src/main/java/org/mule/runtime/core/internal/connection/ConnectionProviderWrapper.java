/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.config.HasPoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for wrappers for {@link ConnectionProvider} instances
 *
 * @param <Config>     the generic type of the configs that the {@link #delegate} accepts
 * @param <Connection> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
public abstract class ConnectionProviderWrapper<Config, Connection> implements ConnectionProvider<Config, Connection>, HasPoolingProfile, Lifecycle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProviderWrapper.class);

    @Inject
    protected MuleContext muleContext;

    private final ConnectionProvider<Config, Connection> delegate;

    /**
     * Creates a new instance which wraps the {@code delegate}
     *
     * @param delegate the {@link ConnectionProvider} to be wrapped
     */
    ConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public Connection connect(Config config) throws ConnectionException
    {
        return delegate.connect(config);
    }

    /**
     * Delegates the connection validation to the delegated {@link ConnectionProvider}
     *
     * @param connection a non {@code null} {@link Connection}.
     * @return the {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
     */
    @Override
    public ConnectionValidationResult validate(Connection connection)
    {
        return delegate.validate(connection);
    }

    @Override
    public void disconnect(Connection connection)
    {
        delegate.disconnect(connection);
    }

    @Override
    public ConnectionHandlingStrategy<Connection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Connection> handlingStrategyFactory)
    {
        return delegate.getHandlingStrategy(handlingStrategyFactory);
    }

    public ConnectionProvider<Config, Connection> getDelegate()
    {
        return delegate;
    }

    /**
     * @return a {@link RetryPolicyTemplate}
     */
    public abstract RetryPolicyTemplate getRetryPolicyTemplate();


    @Override
    public void initialise() throws InitialisationException
    {
        initialiseIfNeeded(delegate, true, muleContext);
        initialiseIfNeeded(getRetryPolicyTemplate(), true, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        startIfNeeded(delegate);
        startIfNeeded(getRetryPolicyTemplate());
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(delegate);
        stopIfNeeded(getRetryPolicyTemplate());
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(delegate, LOGGER);
        disposeIfNeeded(getRetryPolicyTemplate(), LOGGER);
    }
}
