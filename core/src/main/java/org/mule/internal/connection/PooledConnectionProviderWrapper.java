/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.api.connection.PoolingListener;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate}
 * with a user configured {@link PoolingProfile}.
 * <p>
 * The purpose of this wrapper is having the {@link #getHandlingStrategy(ConnectionHandlingStrategyFactory)}
 * method use the configured {@link #poolingProfile} instead of the default included
 * in the {@link #delegate}
 * <p>
 * If a {@link #poolingProfile} is not supplied (meaning, it is {@code null}), then the
 * default {@link #delegate} behavior is applied.
 *
 * @since 4.0
 */
public final class PooledConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection> implements Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PooledConnectionProviderWrapper.class);
    private final boolean disableValidation;
    private final RetryPolicyTemplate retryPolicyTemplate;
    private final PoolingProfile poolingProfile;

    @Inject
    MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param delegate            the {@link ConnectionProvider} to be wrapped
     * @param poolingProfile      a nullable {@link PoolingProfile}
     * @param retryPolicyTemplate a {@link AbstractPolicyTemplate} which will hold the retry policy configured in the Mule Application
     */
    public PooledConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate, PoolingProfile poolingProfile, boolean disableValidation, RetryPolicyTemplate retryPolicyTemplate)
    {
        super(delegate);
        this.poolingProfile = poolingProfile;
        this.disableValidation = disableValidation;
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    /**
     * Delegates the responsibility of validating the connection to the delegated {@link ConnectionProvider}
     * If {@link #disableValidation} if {@code true} then the validation is skipped, returning {@link ConnectionValidationResult#success()}
     *
     * @param connection a given connection
     * @return A {@link ConnectionValidationResult} returned by the delegated {@link ConnectionProvider}
     */
    @Override
    public ConnectionValidationResult validate(Connection connection)
    {
        if (disableValidation)
        {
            return ConnectionValidationResult.success();
        }
        return getDelegate().validate(connection);
    }

    /**
     * If {@link #poolingProfile} is not {@code null} and the delegate wants to invoke
     * {@link ConnectionHandlingStrategyFactory#requiresPooling(PoolingProfile)} or
     * {@link ConnectionHandlingStrategyFactory#supportsPooling(PoolingProfile)}, then this method
     * makes those invokations using the supplied {@link #poolingProfile}.
     * <p>
     * In any other case, the default {@link #delegate} behavior is applied
     *
     * @param handlingStrategyFactory a {@link ConnectionHandlingStrategyFactory}
     * @return a {@link ConnectionHandlingStrategy}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Connection> handlingStrategyFactory)
    {
        ConnectionHandlingStrategyFactory<Config, Connection> factoryDecorator = new ConnectionHandlingStrategyFactoryWrapper<Config, Connection>(handlingStrategyFactory)
        {
            public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
            {
                return super.supportsPooling(resolvePoolingProfile(defaultPoolingProfile), poolingListener);
            }

            @Override
            public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingProfile defaultPoolingProfile)
            {
                return super.supportsPooling(resolvePoolingProfile(defaultPoolingProfile));
            }

            @Override
            public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
            {
                return super.requiresPooling(resolvePoolingProfile(defaultPoolingProfile), poolingListener);
            }

            @Override
            public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile)
            {
                return super.requiresPooling(resolvePoolingProfile(defaultPoolingProfile));
            }

            private PoolingProfile resolvePoolingProfile(PoolingProfile defaultPoolingProfile)
            {
                return poolingProfile != null ? poolingProfile : defaultPoolingProfile;
            }
        };

        return super.getHandlingStrategy(factoryDecorator);
    }

    /**
     * @return a {@link RetryPolicyTemplate} with the configured values in the Mule Application.
     */
    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
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

    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(retryPolicyTemplate);
    }
}
