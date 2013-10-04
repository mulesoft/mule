/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.context.notification.MuleContextListener;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation that uses {@link DefaultMuleContextBuilder} to build new 
 * {@link MuleContext} instances.
 */
public class DefaultMuleContextFactory implements MuleContextFactory
{
    protected static final Log logger = LogFactory.getLog(DefaultMuleContextBuilder.class);

    private List<MuleContextListener> listeners = new LinkedList<MuleContextListener>();

    /**
     * Use default ConfigurationBuilder, default MuleContextBuilder
     */
    public MuleContext createMuleContext() throws InitialisationException, ConfigurationException
    {
        // Configure with defaults needed for a feasible/startable MuleContext
        return createMuleContext(new DefaultsConfigurationBuilder(), new DefaultMuleContextBuilder());
    }

    /**
     * Use default MuleContextBuilder
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder)
        throws InitialisationException, ConfigurationException
    {
        // Create MuleContext using default MuleContextBuilder
        return createMuleContext(configurationBuilder, new DefaultMuleContextBuilder());
    }

    /**
     * Use default ConfigurationBuilder
     */
    public MuleContext createMuleContext(MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException
    {
        // Configure with defaults needed for a feasible/startable MuleContext
        return createMuleContext(new DefaultsConfigurationBuilder(), muleContextBuilder);
    }

    /**
     * {@inheritDoc}
     */
    public MuleContext createMuleContext(List<ConfigurationBuilder> configurationBuilders, 
        MuleContextBuilder muleContextBuilder) throws InitialisationException, ConfigurationException
    {
        // Create MuleContext
        MuleContext muleContext = doCreateMuleContext(muleContextBuilder);

        // Configure
        for (ConfigurationBuilder configBuilder : configurationBuilders)
        {
            configBuilder.configure(muleContext);
        }

        notifyMuleContextConfiguration(muleContext);

        return muleContext;
    }

    /**
     * {@inheritDoc}
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder,
                                         MuleContextBuilder muleContextBuilder)
        throws InitialisationException, ConfigurationException
    {
        // Create MuleContext
        MuleContext muleContext = doCreateMuleContext(muleContextBuilder);

        // Configure
        configurationBuilder.configure(muleContext);

        notifyMuleContextConfiguration(muleContext);

        return muleContext;
    }

    // Additional Factory methods provided by this implementation.

    /**
     * Creates a new {@link MuleContext} instance from the resource provided.
     * Implementations of {@link MuleContextFactory} can either use a default
     * {@link ConfigurationBuilder} to implement this, or do some auto-detection to
     * determine the {@link ConfigurationBuilder} that should be used.
     *
     * @param resource comma seperated list of configuration resources.
     * @throws InitialisationException
     * @throws ConfigurationException
     */
    public MuleContext createMuleContext(String resource)
        throws InitialisationException, ConfigurationException
    {
        return createMuleContext(resource, null);
    }

    /**
     * Creates a new {@link MuleContext} instance from the resource provided.
     * Implementations of {@link MuleContextFactory} can either use a default
     * {@link ConfigurationBuilder} to implement this, or do some auto-detection to
     * determine the {@link ConfigurationBuilder} that should be used. Properties if
     * provided are used to replace "property placeholder" value in configuration
     * files.
     */
    public MuleContext createMuleContext(String configResources, Properties properties)
        throws InitialisationException, ConfigurationException
    {
        // Create MuleContext
        MuleContext muleContext = doCreateMuleContext(new DefaultMuleContextBuilder());

        // Configure with startup properties
        if (properties != null && !properties.isEmpty())
        {
            new SimpleConfigurationBuilder(properties).configure(muleContext);
        }

        // Automatically resolve Configuration to be used and delegate configuration
        // to it.
        new AutoConfigurationBuilder(configResources).configure(muleContext);

        notifyMuleContextConfiguration(muleContext);

        return muleContext;
    }

    /**
     * Creates a new MuleContext using the given configurationBuilder. Properties if
     * provided are used to replace "property placeholder" value in configuration
     * files.
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder, Properties properties)
        throws InitialisationException, ConfigurationException
    {
        return createMuleContext(configurationBuilder, properties, new DefaultMuleConfiguration());
    }

    /**
     * Creates a new MuleContext using the given configurationBuilder and configuration. Properties if
     * provided are used to replace "property placeholder" value in configuration
     * files.
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder,
                                         Properties properties,
                                         MuleConfiguration configuration)
        throws InitialisationException, ConfigurationException
    {
        // Create MuleContext
        DefaultMuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        contextBuilder.setMuleConfiguration(configuration);
        MuleContext muleContext = doCreateMuleContext(contextBuilder);

        // Configure with startup properties
        if (properties != null && !properties.isEmpty())
        {
            new SimpleConfigurationBuilder(properties).configure(muleContext);
        }

        // Configure with configurationBuilder
        configurationBuilder.configure(muleContext);

        notifyMuleContextConfiguration(muleContext);

        return muleContext;
    }

    protected MuleContext doCreateMuleContext(MuleContextBuilder muleContextBuilder)
        throws InitialisationException
    {
        // Create muleContext instance and set it in MuleServer
        MuleContext muleContext = buildMuleContext(muleContextBuilder);

        notifyMuleContextCreation(muleContext);

        // Initialiase MuleContext
        muleContext.initialise();

        notifyMuleContextInitialization(muleContext);

        return muleContext;
    }

    protected MuleContext buildMuleContext(MuleContextBuilder muleContextBuilder)
    {
        return muleContextBuilder.buildMuleContext();
    }

    @Override
    public void addListener(MuleContextListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(MuleContextListener listener)
    {
        return listeners.remove(listener);
    }

    private void notifyMuleContextCreation(MuleContext context)
    {
        for (MuleContextListener listener : listeners)
        {
            listener.onCreation(context);
        }
    }

    private void notifyMuleContextInitialization(MuleContext context)
    {
        for (MuleContextListener listener : listeners)
        {
            listener.onInitialization(context);
        }
    }

    private void notifyMuleContextConfiguration(MuleContext context)
    {
        for (MuleContextListener listener : listeners)
        {
            listener.onConfiguration(context);
        }
    }
}
