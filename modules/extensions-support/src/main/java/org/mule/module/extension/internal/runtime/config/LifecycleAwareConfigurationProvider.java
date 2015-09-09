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
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.ConfigurationInstance;
import org.mule.lifecycle.DefaultLifecycleManager;
import org.mule.lifecycle.SimpleLifecycleManager;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementations of {@link ConfigurationProvider} which keep track of the
 * {@link ConfigurationInstance} they generate and propagate lifecycle and IoC into them.
 * <p/>
 * It also implements the other common concerns of every {@link ConfigurationProvider}, leaving implementations
 * with the need to &quot;just&quot; implement {@link #get(Object)}
 *
 * @param <T> the generic type for the supplied {@link ConfigurationInstance}
 * @since 4.0
 */
public abstract class LifecycleAwareConfigurationProvider<T> implements ConfigurationProvider<T>, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAwareConfigurationProvider.class);

    private final String name;
    private final ConfigurationModel configurationModel;
    private final List<ConfigurationInstance<T>> configurationInstances = new LinkedList<>();
    protected SimpleLifecycleManager lifecycleManager = new DefaultLifecycleManager<>(String.format("%s-%s", getClass().getName(), getName()), this);

    @Inject
    protected MuleContext muleContext;

    public LifecycleAwareConfigurationProvider(String name, ConfigurationModel configurationModel)
    {
        this.name = name;
        this.configurationModel = configurationModel;
    }

    /**
     * Performs dependency injection into all the currently provided configurations,
     * and when needed, fires the {@link Initialisable#initialise()} phase on them
     *
     * @throws InitialisationException if an exception is found
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            lifecycleManager.fireInitialisePhase((phaseName, object) -> {
                for (ConfigurationInstance<T> configurationInstance : configurationInstances)
                {
                    muleContext.getInjector().inject(configurationInstance);
                    initialiseConfig(configurationInstance);
                }
            });
        }
        catch (MuleException e)
        {
            throw (InitialisationException) e;
        }
    }

    /**
     * When needed, fires the {@link Startable#start()} phase on the currently provided configurations
     *
     * @throws MuleException if an exception is found
     */
    @Override
    public void start() throws MuleException
    {
        lifecycleManager.fireStartPhase((phaseName, object) -> {
            for (ConfigurationInstance<T> configurationInstance : configurationInstances)
            {
                startConfig(configurationInstance);
            }
        });
    }

    /**
     * When needed, fires the {@link Stoppable#stop()} phase on the currently provided configurations
     *
     * @throws MuleException if an exception is found
     */
    @Override
    public void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase((phaseName, object) -> {
            for (ConfigurationInstance<T> configurationInstance : configurationInstances)
            {
                stopIfNeeded(configurationInstance);
            }

        });
    }

    /**
     * When needed, fires the {@link Disposable#dispose()} phase on the currently provided configurations
     */
    @Override
    public void dispose()
    {
        try
        {
            lifecycleManager.fireDisposePhase((phaseName, object) -> {
                for (ConfigurationInstance<T> configurationInstance : configurationInstances)
                {
                    disposeIfNeeded(configurationInstance, LOGGER);
                }
            });

        }
        catch (MuleException e)
        {
            LOGGER.error("Could not dispose configuration provider of name " + getName(), e);
        }
    }

    /**
     * Implementations are to invoke this method everytime they create a new {@link ConfigurationInstance}
     * so that they're kept track of and the lifecycle can be propagated
     *
     * @param configuration a newly created {@link ConfigurationInstance}
     */
    protected void registerConfiguration(ConfigurationInstance<T> configuration)
    {
        configurationInstances.add(configuration);
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
        return configurationModel;
    }

    protected void initialiseConfig(ConfigurationInstance<T> config) throws InitialisationException
    {
        try
        {
            muleContext.getInjector().inject(config);
            initialiseIfNeeded(config, muleContext);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void startConfig(ConfigurationInstance<T> config) throws MuleException
    {
        startIfNeeded(config);
    }

    void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
