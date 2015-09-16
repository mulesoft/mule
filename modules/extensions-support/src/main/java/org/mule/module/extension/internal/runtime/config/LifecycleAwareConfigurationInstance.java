/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.util.Preconditions.checkState;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.Interceptable;
import org.mule.extension.runtime.ConfigurationStats;
import org.mule.extension.runtime.ConfigurationInstance;
import org.mule.extension.runtime.Interceptor;
import org.mule.time.TimeSupplier;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConfigurationInstance} which propagates dependency injection
 * and lifecycle phases into the contained configuration value {@link #value}
 * <p/>
 * It also implements the {@link Interceptable} interface which means that it contains
 * a list of {@link Interceptor interceptors}, on which IoC and lifecycle is propagated
 * as well
 *
 * @since 4.0
 */
public final class LifecycleAwareConfigurationInstance<T> implements ConfigurationInstance<T>, Lifecycle, Interceptable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAwareConfigurationInstance.class);

    private final String name;
    private final ConfigurationModel model;
    private final T value;
    private final List<Interceptor> interceptors;

    private ConfigurationStats configurationStats;

    @Inject
    private MuleContext muleContext;

    @Inject
    private TimeSupplier timeSupplier;

    /**
     * Creates a new instance
     *
     * @param name         this configuration's name
     * @param model        the {@link ConfigurationModel} for this instance
     * @param value        the actual configuration instance
     * @param interceptors the {@link List} of {@link Interceptor interceptors} that applies
     */
    public LifecycleAwareConfigurationInstance(String name, ConfigurationModel model, T value, List<Interceptor> interceptors)
    {
        this.name = name;
        this.model = model;
        this.value = value;
        this.interceptors = interceptors != null ? ImmutableList.copyOf(interceptors) : ImmutableList.of();
    }

    /**
     * Initialises this instance by
     * <ul>
     * <li>Initialising the {@link #configurationStats}</li>
     * <li>Performs dependency injection on the {@link #value} and each item in {@link #getInterceptors()}</li>
     * <li>Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}</li>
     * </ul>
     *
     * @throws InitialisationException if an exception is found
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            initStats();
            inject();
            doInitialise();
        }
        catch (Exception e)
        {
            if (e instanceof InitialisationException)
            {
                throw (InitialisationException) e;
            }
            else
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    /**
     * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
     *
     * @throws MuleException if an exception is found
     */
    @Override
    public void start() throws MuleException
    {
        startIfNeeded(value);
        for (Interceptor interceptor : interceptors)
        {
            startIfNeeded(interceptor);
        }
    }

    /**
     * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
     *
     * @throws MuleException if an exception is found
     */
    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(value);
        for (Interceptor interceptor : interceptors)
        {
            stopIfNeeded(interceptor);
        }
    }

    /**
     * Propagates this lifecycle phase into the the {@link #value} and each item in {@link #getInterceptors()}
     */
    @Override
    public void dispose()
    {
        disposeIfNeeded(value, LOGGER);
        for (Interceptor interceptor : interceptors)
        {
            disposeIfNeeded(interceptor, LOGGER);
        }
    }

    private void doInitialise() throws InitialisationException
    {
        initialiseIfNeeded(value, muleContext);
        for (Interceptor interceptor : interceptors)
        {
            initialiseIfNeeded(interceptor, muleContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationModel getModel()
    {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue()
    {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if invoked before {@link #initialise()}
     */
    @Override
    public ConfigurationStats getStatistics()
    {
        checkState(configurationStats != null, "can't get statistics before initialise() is invoked");
        return configurationStats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Interceptor> getInterceptors()
    {
        return interceptors;
    }

    private void initStats()
    {
        if (timeSupplier == null)
        {
            timeSupplier = new TimeSupplier();
        }

        configurationStats = new DefaultMutableConfigurationStats(timeSupplier);
    }

    private void inject() throws MuleException
    {
        muleContext.getInjector().inject(value);
        for (Interceptor interceptor : interceptors)
        {
            muleContext.getInjector().inject(interceptor);
        }
    }

    void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    void setTimeSupplier(TimeSupplier timeSupplier)
    {
        this.timeSupplier = timeSupplier;
    }
}
