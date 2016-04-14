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
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.retry.RetryPolicyTemplate;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate}
 * with a user configured {@link #disableValidation} flag value.
 * <p>
 * The purpose of this class is, in case of a {@link #disableValidation} with {@code true} as value,
 * is to delegate the validation the actual connection to the {@link #delegate}. If {@link #disableValidation} is {@code false},
 * the validation will return a {@link ConnectionValidationResult} with a valid status.
 *
 * @since 4.0
 */
public final class CachedConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection> implements Lifecycle, MuleContextAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PooledConnectionProviderWrapper.class);
    private final boolean disableValidation;
    private final RetryPolicyTemplate retryPolicyTemplate;

    @Inject
    private MuleContext muleContext;

    public CachedConnectionProviderWrapper(ConnectionProvider<Config, Connection> provider, boolean disableValidation, RetryPolicyTemplate retryPolicyTemplate)
    {
        super(provider);
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
     * @return a {@link RetryPolicyTemplate} with the configured values in the Mule Application.
     */
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
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

    @Override
    public Optional<PoolingProfile> getPoolingProfile()
    {
        return Optional.empty();
    }
}

