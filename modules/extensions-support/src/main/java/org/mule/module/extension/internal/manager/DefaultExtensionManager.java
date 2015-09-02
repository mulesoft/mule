/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.module.extension.internal.manager.DefaultConfigurationExpirationMonitor.Builder.newBuilder;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.config.ExtensionConfig;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.config.StaticConfigurationProvider;
import org.mule.registry.SpiServiceRegistry;
import org.mule.time.Time;
import org.mule.util.ObjectNameHelper;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManagerAdapter}. This implementation uses standard Java SPI
 * as a discovery mechanism
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManagerAdapter, MuleContextAware, Initialisable, Startable, Stoppable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

    private final ExtensionRegistry extensionRegistry = new ExtensionRegistry();
    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();

    private MuleContext muleContext;
    private ObjectNameHelper objectNameHelper;
    private ExtensionDiscoverer extensionDiscoverer = new DefaultExtensionDiscoverer(new DefaultExtensionFactory(serviceRegistry), serviceRegistry);
    private ImplicitConfigurationFactory implicitConfigurationFactory;
    private ConfigurationExpirationMonitor configurationExpirationMonitor;

    @Override
    public void initialise() throws InitialisationException
    {
        objectNameHelper = new ObjectNameHelper(muleContext);
        implicitConfigurationFactory = new DefaultImplicitConfigurationFactory(extensionRegistry, muleContext);
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
            throw new IllegalArgumentException(String.format("A extension of name '%s' (version %s) is already registered",
                                                             extensionModel.getName(), extensionModel.getVersion()));
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
    public <C> void registerConfigurationProvider(ExtensionModel extensionModel, String providerName, ConfigurationProvider<C> configurationProvider)
    {
        ExtensionStateTracker extensionStateTracker = extensionRegistry.getExtensionState(extensionModel);
        extensionStateTracker.registerConfigurationProvider(providerName, configurationProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfiguration(ExtensionModel extensionModel, String configurationProviderName, OperationContext operationContext)
    {
        ConfigurationProvider<C> configurationProvider = getConfigurationProvider(extensionModel, configurationProviderName);
        return configurationProvider.get(operationContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfiguration(ExtensionModel extensionModel, OperationContext operationContext)
    {
        List<ConfigurationProvider<?>> providers = extensionRegistry.getExtensionState(extensionModel).getConfigurationProviders();

        int matches = providers.size();

        if (matches == 1)
        {
            ConfigurationProvider<?> provider = providers.get(0);
            return (C) provider.get(operationContext);
        }
        else if (matches > 1)
        {
            throw new IllegalStateException(String.format("No config-ref was specified for operation '%s' of extension '%s', but %d are registered. Please specify which to use",
                                                          operationContext.getOperationModel().getName(), extensionModel.getName(), matches));
        }
        else
        {
            attemptToCreateImplicitConfiguration(extensionModel, operationContext);
            return getConfiguration(extensionModel, operationContext);
        }
    }

    private void attemptToCreateImplicitConfiguration(ExtensionModel extensionModel, OperationContext operationContext)
    {
        ConfigurationHolder configurationHolder = implicitConfigurationFactory.createImplicitConfiguration(extensionModel, operationContext, this);
        if (configurationHolder != null)
        {
            Object configuration = configurationHolder.getConfiguration();
            registerConfigurationProvider(extensionModel, configurationHolder.getName(), new StaticConfigurationProvider<>(configuration));
            registerConfiguration(extensionModel, configurationHolder.getName(), configuration);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Set<ExtensionModel> getExtensionsCapableOf(Class<C> capabilityType)
    {
        checkArgument(capabilityType != null, "capability type cannot be null");
        return extensionRegistry.getExtensionsCapableOf(capabilityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> String registerConfiguration(ExtensionModel extensionModel, String configurationProviderName, C configuration)
    {
        ExtensionStateTracker extensionStateTracker = extensionRegistry.getExtensionState(extensionModel);
        final String registrationName = objectNameHelper.getUniqueName(configurationProviderName);

        extensionStateTracker.registerConfiguration(configurationProviderName, registrationName, configuration);

        putInRegistryAndApplyLifecycle(registrationName, configuration);

        return registrationName;
    }

    private void putInRegistryAndApplyLifecycle(String key, Object object)
    {
        try
        {
            muleContext.getRegistry().registerObject(key, object);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private <C> ConfigurationProvider<C> getConfigurationProvider(ExtensionModel extensionModel, String configurationProviderName)
    {
        ConfigurationProvider<C> configurationProvider = extensionRegistry.getExtensionState(extensionModel).getConfigurationProvider(configurationProviderName);

        if (configurationProvider == null)
        {
            throw new IllegalArgumentException(String.format("There is no registered configurationProvider under name '%s'", configurationProviderName));
        }

        return configurationProvider;
    }

    private ConfigurationExpirationMonitor newConfigurationExpirationMonitor()
    {
        Time freq = getConfigurationExpirationFrequency();
        return newBuilder(extensionRegistry, muleContext)
                .runEvery(freq.getTime(), freq.getUnit())
                .onExpired((key, object) -> unregisterConfiguration(key, object))
                .build();
    }

    private void unregisterConfiguration(String key, Object object)
    {
        try
        {
            muleContext.getRegistry().unregisterObject(key);
        }
        catch (RegistrationException e)
        {
            LOGGER.error(String.format("Could not unregister expired dynamic config of key '%s' and type %s",
                                       key, object.getClass().getName()), e);
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
