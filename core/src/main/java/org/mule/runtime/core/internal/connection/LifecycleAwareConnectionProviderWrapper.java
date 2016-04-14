/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.HasPoolingProfile;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.retry.RetryPolicy;
import org.mule.api.retry.RetryPolicyTemplate;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionProviderWrapper} which performs lifecycle and dependency injection
 * operations on the generated connections
 *
 * @param <Config>     the generic type of the configs that the {@link #delegate} accepts
 * @param <Connection> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
class LifecycleAwareConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection> implements HasPoolingProfile
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAwareConnectionProviderWrapper.class);

    private final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param delegate    the {@link ConnectionProvider} to be wrapped
     * @param muleContext the owning {@link MuleContext}
     */
    LifecycleAwareConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate, MuleContext muleContext)
    {
        super(delegate);
        this.muleContext = muleContext;
    }

    /**
     * Obtains a {@code Connection} from the delegate and applies injection
     * dependency and the {@code muleContext}'s completed {@link Lifecycle}
     * phases
     *
     * @param config a configuration which parametrizes the connection's creation
     * @return a {@code Connection} with dependencies injected and the correct lifecycle state
     * @throws ConnectionException if an exception was found obtaining the connection or managing it
     */
    @Override
    public Connection connect(Config config) throws ConnectionException
    {
        Connection connection = super.connect(config);
        try
        {
            muleContext.getRegistry().applyProcessorsAndLifecycle(connection);
        }
        catch (MuleException e)
        {
            throw new ConnectionException("Could not initialise connection", e);
        }

        return connection;
    }

    /**
     * Disconnects the {@code connection} and then applies all the necessary
     * {@link Lifecycle} phases until the {@link Disposable#PHASE_NAME} transition
     * is reached
     *
     * @param connection the {@code Connection} to be destroyed
     */
    @Override
    public void disconnect(Connection connection)
    {
        try
        {
            super.disconnect(connection);
        }
        finally
        {
            try
            {
                muleContext.getRegistry().applyLifecycle(connection, Disposable.PHASE_NAME);
            }
            catch (MuleException e)
            {
                LOGGER.warn("Exception was found trying to dispose connection", e);
            }
        }
    }

    /**
     * @return a {@link RetryPolicyTemplate} from the delegated {@link ConnectionProviderWrapper}, if the {@link #delegate}
     * is a {@link ConnectionProvider} then a default {@link RetryPolicy} is returned from the {@link ConnectionProviderWrapper}
     */
    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        final ConnectionProvider<Config, Connection> delegate = getDelegate();
        if (delegate instanceof ConnectionProviderWrapper)
        {
            return ((ConnectionProviderWrapper) delegate).getRetryPolicyTemplate();
        }

        return ((ConnectionManagerAdapter) muleContext.getRegistry().get(MuleProperties.OBJECT_CONNECTION_MANAGER)).getDefaultRetryPolicyTemplate();
    }

    @Override
    public Optional<PoolingProfile> getPoolingProfile()
    {
        ConnectionProvider<Config, Connection> delegate = getDelegate();
        return delegate instanceof ConnectionProviderWrapper ? ((ConnectionProviderWrapper) delegate).getPoolingProfile() : Optional.empty();
    }
}
