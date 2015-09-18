/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.module.extension.internal.manager.DefaultConfigurationExpirationMonitor.Builder.newBuilder;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.module.extension.internal.config.ExtensionConfig;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.config.StaticConfigurationProvider;
import org.mule.registry.SpiServiceRegistry;
import org.mule.time.Time;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManager}. This implementation uses standard Java SPI
 * as a discovery mechanism.
 * <p/>
 * Although it allows registering {@link ConfigurationProvider} instances through the
 * {@link #registerConfigurationProvider(ConfigurationProvider)} method (and that's still the
 * correct way of registering them), this implementation automatically acknowledges any
 * {@link ConfigurationProvider} already present on the {@link MuleRegistry}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManager, MuleContextAware, Initialisable, Startable, Stoppable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();

    private MuleContext muleContext;
    private ExtensionRegistry extensionRegistry;
    private ExtensionDiscoverer extensionDiscoverer;
    private ImplicitConfigurationFactory implicitConfigurationFactory;
    private ConfigurationExpirationMonitor configurationExpirationMonitor;


    @Override
    public void initialise() throws InitialisationException
    {
        extensionRegistry = new ExtensionRegistry(muleContext.getRegistry());
        if (extensionDiscoverer == null)
        {
            extensionDiscoverer = new DefaultExtensionDiscoverer(new DefaultExtensionFactory(serviceRegistry, muleContext.getExecutionClassLoader()), serviceRegistry);
        }

        implicitConfigurationFactory = new DefaultImplicitConfigurationFactory(muleContext.getExpressionManager());
    }

    /**
     * Starts the {@link #configurationExpirationMonitor}
     *
     * @throws MuleException if it fails to start
     */
    @Override
    public void start() throws MuleException
    {
        configurationExpirationMonitor = newConfigurationExpirationMonitor();
        configurationExpirationMonitor.beginMonitoring();
    }

    /**
     * Stops the {@link #configurationExpirationMonitor}
     *
     * @throws MuleException if it fails to stop
     */
    @Override
    public void stop() throws MuleException
    {
        configurationExpirationMonitor.stopMonitoring();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionModel> discoverExtensions(ClassLoader classLoader)
    {
        LOGGER.info("Starting discovery of extensions");

        List<ExtensionModel> discovered = extensionDiscoverer.discover(classLoader);
        LOGGER.info("Discovered {} extensions", discovered.size());

        discovered.forEach(this::registerExtension);
        return ImmutableList.copyOf(extensionRegistry.getExtensions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerExtension(ExtensionModel extensionModel)
    {
        LOGGER.info("Registering extension {} (version {})", extensionModel.getName(), extensionModel.getVersion());
        final String extensionName = extensionModel.getName();

        if (extensionRegistry.containsExtension(extensionName))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("A extension of name '{}' (version {}) is already registered. Skipping...",
                             extensionModel.getName(), extensionModel.getVersion());
            }
        }
        else
        {
            extensionRegistry.registerExtension(extensionName, extensionModel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void registerConfigurationProvider(ConfigurationProvider<C> configurationProvider)
    {
        extensionRegistry.registerConfigurationProvider(configurationProvider);
    }

    /**
     * {@inheritDoc}
     */
    //TODO: muleEvent should actually be of MuleEvent type when mule-api jar becomes available
    @Override
    public <C> ConfigurationInstance<C> getConfiguration(String configurationProviderName, Object muleEvent)
    {
        checkArgument(!StringUtils.isBlank(configurationProviderName), "cannot get configuration from a blank provider name");
        ConfigurationProvider<C> configurationProvider = extensionRegistry.getConfigurationProvider(configurationProviderName);
        return configurationProvider.get(muleEvent);
    }

    /**
     * {@inheritDoc}
     */
    //TODO: muleEvent should actually be of MuleEvent type when mule-api jar becomes available
    @Override
    public <C> ConfigurationInstance<C> getConfiguration(ExtensionModel extensionModel, Object muleEvent)
    {

        List<ConfigurationProvider> providers = extensionRegistry.getConfigurationProviders(extensionModel);

        int matches = providers.size();

        if (matches == 1)
        {
            return providers.get(0).get(muleEvent);
        }
        else if (matches > 1)
        {
            throw new IllegalStateException(String.format("No config-ref was specified for operation of extension '%s', but %d are registered. Please specify which to use",
                                                          extensionModel.getName(),
                                                          matches));
        }
        else
        {
            if (attemptToCreateImplicitConfiguration(extensionModel, (MuleEvent) muleEvent))
            {
                return getConfiguration(extensionModel, muleEvent);
            }

            throw new IllegalStateException(String.format("No config-ref was specified for operation of extension '%s' and no implicit configuration could be inferred. Please define one.",
                                                          extensionModel.getName()));
        }
    }

    private boolean attemptToCreateImplicitConfiguration(ExtensionModel extensionModel, MuleEvent muleEvent)
    {
        synchronized (extensionModel)
        {
            //check that another thread didn't beat us to create the instance
            if (!extensionRegistry.getConfigurationProviders(extensionModel).isEmpty())
            {
                return true;
            }
            ConfigurationInstance<Object> configurationInstance = implicitConfigurationFactory.createImplicitConfigurationInstance(extensionModel, muleEvent);
            if (configurationInstance != null)
            {
                ConfigurationProvider<Object> configurationProvider = new StaticConfigurationProvider<>(configurationInstance.getName(), configurationInstance.getModel(), configurationInstance);
                registerConfigurationProvider(configurationProvider);

                return true;
            }

            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ExtensionModel> getExtensions()
    {
        return extensionRegistry.getExtensions();
    }

    private ConfigurationExpirationMonitor newConfigurationExpirationMonitor()
    {
        Time freq = getConfigurationExpirationFrequency();
        return newBuilder(extensionRegistry, muleContext)
                .runEvery(freq.getTime(), freq.getUnit())
                .onExpired((key, object) -> disposeConfiguration(key, object))
                .build();
    }

    private void disposeConfiguration(String key, ConfigurationInstance<Object> configuration)
    {
        try
        {
            stopIfNeeded(configuration);
            disposeIfNeeded(configuration, LOGGER);
        }
        catch (Exception e)
        {
            LOGGER.error(String.format("Could not dispose expired dynamic config of key '%s' and type %s", key, configuration.getClass().getName()), e);
        }
    }

    private Time getConfigurationExpirationFrequency()
    {
        ExtensionConfig extensionConfig = muleContext.getConfiguration().getExtension(ExtensionConfig.class);
        if (extensionConfig != null)
        {
            return extensionConfig.getDynamicConfigExpirationFrequency();
        }
        else
        {
            return new Time(5L, TimeUnit.MINUTES);
        }
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    void setExtensionsDiscoverer(ExtensionDiscoverer discoverer)
    {
        extensionDiscoverer = discoverer;
    }
}
