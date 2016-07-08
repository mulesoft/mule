/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.AutoConfigurationBuilder;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation that uses {@link DefaultMuleContextBuilder} to build new
 * {@link MuleContext} instances.
 */
public class DefaultMuleContextFactory implements MuleContextFactory
{

    protected static final Logger logger = LoggerFactory.getLogger(DefaultMuleContextBuilder.class);

    private List<MuleContextListener> listeners = new LinkedList<MuleContextListener>();

    /**
     * Use default ConfigurationBuilder, default MuleContextBuilder
     */
    public MuleContext createMuleContext() throws InitialisationException, ConfigurationException
    {
        // Configure with defaults needed for a feasible/startable MuleContext
        return createMuleContext(new DefaultsConfigurationBuilder(), createMuleContextBuilder());
    }

    protected DefaultMuleContextBuilder createMuleContextBuilder()
    {
        return new DefaultMuleContextBuilder();
    }

    /**
     * Use default MuleContextBuilder
     */
    public MuleContext createMuleContext(ConfigurationBuilder configurationBuilder)
            throws InitialisationException, ConfigurationException
    {
        // Create MuleContext using default MuleContextBuilder
        return createMuleContext(configurationBuilder, createMuleContextBuilder());
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
    public MuleContext createMuleContext(final List<ConfigurationBuilder> configurationBuilders,
                                         MuleContextBuilder muleContextBuilder) throws InitialisationException, ConfigurationException
    {
        return doCreateMuleContext(muleContextBuilder, new ContextConfigurator()
        {
            @Override
            public void configure(MuleContext muleContext) throws ConfigurationException
            {
                // Configure
                for (ConfigurationBuilder configBuilder : configurationBuilders)
                {
                    configBuilder.configure(muleContext);
                }
                notifyMuleContextConfiguration(muleContext);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public MuleContext createMuleContext(final ConfigurationBuilder configurationBuilder,
                                         MuleContextBuilder muleContextBuilder)
            throws InitialisationException, ConfigurationException
    {
        return doCreateMuleContext(muleContextBuilder, new ContextConfigurator()
        {
            @Override
            public void configure(MuleContext muleContext) throws ConfigurationException
            {
                configurationBuilder.configure(muleContext);

                notifyMuleContextConfiguration(muleContext);
            }
        });
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
    public MuleContext createMuleContext(final String configResources, final Properties properties)
            throws InitialisationException, ConfigurationException
    {
        return doCreateMuleContext(createMuleContextBuilder(), new ContextConfigurator()
        {
            @Override
            public void configure(MuleContext muleContext) throws ConfigurationException
            {
                // Configure with startup properties
                if (properties != null && !properties.isEmpty())
                {
                    new SimpleConfigurationBuilder(properties).configure(muleContext);
                }

                // Automatically resolve Configuration to be used and delegate configuration
                // to it.
                new AutoConfigurationBuilder(configResources, emptyMap(), APP).configure(muleContext);

                notifyMuleContextConfiguration(muleContext);
            }
        });
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
    public MuleContext createMuleContext(final ConfigurationBuilder configurationBuilder,
                                         final Properties properties,
                                         MuleConfiguration configuration)
            throws InitialisationException, ConfigurationException
    {
        // Create MuleContext
        DefaultMuleContextBuilder contextBuilder = createMuleContextBuilder();
        contextBuilder.setMuleConfiguration(configuration);
        return doCreateMuleContext(contextBuilder, new ContextConfigurator()
        {
            @Override
            public void configure(MuleContext muleContext) throws ConfigurationException
            {
                // Configure with startup properties
                if (properties != null && !properties.isEmpty())
                {
                    new SimpleConfigurationBuilder(properties).configure(muleContext);
                }

                // Configure with configurationBuilder
                configurationBuilder.configure(muleContext);

                notifyMuleContextConfiguration(muleContext);
            }
        });
    }

    protected MuleContext doCreateMuleContext(MuleContextBuilder muleContextBuilder, ContextConfigurator configurator)
            throws InitialisationException, ConfigurationException
    {
        // Create muleContext instance and set it in MuleServer
        MuleContext muleContext = buildMuleContext(muleContextBuilder);

        notifyMuleContextCreation(muleContext);

        // Initialiase MuleContext
        muleContext.initialise();

        notifyMuleContextInitialization(muleContext);

        try
        {
            configurator.configure(muleContext);
        }
        catch (ConfigurationException e)
        {
            if (muleContext != null && !muleContext.isDisposed())
            {
                try
                {
                    muleContext.dispose();
                }
                catch (Exception e1)
                {
                    logger.warn("Can not dispose context. " + ExceptionUtils.getMessage(e1));
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Can not dispose context. " + ExceptionUtils.getFullStackTrace(e1));
                    }
                }
            }
            throw e;
        }
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

    private abstract class ContextConfigurator
    {

        public abstract void configure(MuleContext muleContext) throws ConfigurationException;
    }
}
