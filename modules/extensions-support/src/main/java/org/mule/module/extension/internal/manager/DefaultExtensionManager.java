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
import org.mule.common.MuleVersion;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.config.ExtensionConfig;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.config.StaticConfigurationInstanceProvider;
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
    public List<Extension> discoverExtensions(ClassLoader classLoader)
    {
        LOGGER.info("Starting discovery of extensions");

        List<Extension> discovered = extensionDiscoverer.discover(classLoader);
        LOGGER.info("Discovered {} extensions", discovered.size());

        discovered.forEach(this::registerExtension);
        return ImmutableList.copyOf(extensionRegistry.getExtensions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerExtension(Extension extension)
    {
        LOGGER.info("Registering extension {} (version {})", extension.getName(), extension.getVersion());
        final String extensionName = extension.getName();

        if (extensionRegistry.containsExtension(extensionName))
        {
            return maybeUpdateExtension(extension, extensionName);
        }
        else
        {
            doRegisterExtension(extension, extensionName);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void registerConfigurationInstanceProvider(Extension extension, String providerName, ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        ExtensionStateTracker extensionStateTracker = extensionRegistry.getExtensionState(extension);
        extensionStateTracker.registerConfigurationInstanceProvider(providerName, configurationInstanceProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfigurationInstance(Extension extension, String configurationInstanceProviderName, OperationContext operationContext)
    {
        ConfigurationInstanceProvider<C> configurationInstanceProvider = getConfigurationInstanceProvider(extension, configurationInstanceProviderName);
        return configurationInstanceProvider.get(operationContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfigurationInstance(Extension extension, OperationContext operationContext)
    {
        List<ConfigurationInstanceProvider<?>> providers = extensionRegistry.getExtensionState(extension).getConfigurationInstanceProviders();

        int matches = providers.size();

        if (matches == 1)
        {
            ConfigurationInstanceProvider<?> provider = providers.get(0);
            return (C) provider.get(operationContext);
        }
        else if (matches > 1)
        {
            throw new IllegalStateException(String.format("No config-ref was specified for operation '%s' of extension '%s', but %d are registered. Please specify which to use",
                                                          operationContext.getOperation().getName(), extension.getName(), matches));
        }
        else
        {
            attemptToCreateImplicitConfigurationInstance(extension, operationContext);
            return getConfigurationInstance(extension, operationContext);
        }
    }

    private void attemptToCreateImplicitConfigurationInstance(Extension extension, OperationContext operationContext)
    {
        ConfigurationInstanceHolder configurationInstanceHolder = implicitConfigurationFactory.createImplicitConfigurationInstance(extension, operationContext, this);
        if (configurationInstanceHolder != null)
        {
            Object configurationInstance = configurationInstanceHolder.getConfigurationInstance();
            registerConfigurationInstanceProvider(extension, configurationInstanceHolder.getName(), new StaticConfigurationInstanceProvider<>(configurationInstance));
            registerConfigurationInstance(extension, configurationInstanceHolder.getName(), configurationInstance);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Extension> getExtensions()
    {
        return extensionRegistry.getExtensions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Set<Extension> getExtensionsCapableOf(Class<C> capabilityType)
    {
        checkArgument(capabilityType != null, "capability type cannot be null");
        return extensionRegistry.getExtensionsCapableOf(capabilityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> String registerConfigurationInstance(Extension extension, String configurationInstanceProviderName, C configurationInstance)
    {
        ExtensionStateTracker extensionStateTracker = extensionRegistry.getExtensionState(extension);
        final String registrationName = objectNameHelper.getUniqueName(configurationInstanceProviderName);

        extensionStateTracker.registerConfigurationInstance(configurationInstanceProviderName, registrationName, configurationInstance);

        putInRegistryAndApplyLifecycle(registrationName, configurationInstance);

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

    private <C> ConfigurationInstanceProvider<C> getConfigurationInstanceProvider(Extension extension, String configurationInstanceProviderName)
    {
        ConfigurationInstanceProvider<C> configurationInstanceProvider = extensionRegistry.getExtensionState(extension).getConfigurationInstanceProvider(configurationInstanceProviderName);

        if (configurationInstanceProvider == null)
        {
            throw new IllegalArgumentException(String.format("There is no registered ConfigurationInstanceProvider under name '%s'", configurationInstanceProviderName));
        }

        return configurationInstanceProvider;
    }

    private boolean maybeUpdateExtension(Extension extension, String extensionName)
    {
        Extension actual = extensionRegistry.getExtension(extensionName);
        MuleVersion newVersion;
        try
        {
            newVersion = new MuleVersion(extension.getVersion());
        }
        catch (IllegalArgumentException e)
        {
            LOGGER.warn(
                    String.format("Found extensions %s with invalid version %s. Skipping registration",
                                  extension.getName(), extension.getVersion()), e);

            return false;
        }

        if (newVersion.newerThan(actual.getVersion()))
        {
            logExtensionHotUpdate(extension, actual);
            doRegisterExtension(extension, extensionName);

            return true;
        }
        else
        {
            LOGGER.info("Found extension {} but version {} was already registered. Keeping existing definition",
                        extension.getName(),
                        extension.getVersion());

            return false;
        }
    }

    private void doRegisterExtension(Extension extension, String extensionName)
    {
        extensionRegistry.registerExtension(extensionName, extension);
    }

    private void logExtensionHotUpdate(Extension extension, Extension actual)
    {
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(String.format(
                    "Found extension %s which was already registered with version %s. New version %s " +
                    "was found. Hot updating extension definition",
                    extension.getName(),
                    actual.getVersion(),
                    extension.getVersion()));
        }
    }

    private ConfigurationExpirationMonitor newConfigurationExpirationMonitor()
    {
        Time freq = getConfigurationExpirationFrequency();
        return newBuilder(extensionRegistry, muleContext)
                .runEvery(freq.getTime(), freq.getUnit())
                .onExpired((key, object) -> unregisterConfigurationInstance(key, object))
                .build();
    }

    private void unregisterConfigurationInstance(String key, Object object)
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
