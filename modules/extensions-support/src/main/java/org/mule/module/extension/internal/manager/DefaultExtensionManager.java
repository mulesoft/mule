/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.asOperationContextAdapter;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.StaticConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.resolver.EvaluateAndTransformValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.registry.SpiServiceRegistry;
import org.mule.util.ObjectNameHelper;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManagerAdapter}. This implementation uses standard Java SPI
 * as a discovery mechanism
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManagerAdapter, MuleContextAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

    private final ExtensionRegistry extensionRegistry = new ExtensionRegistry();
    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();

    private MuleContext muleContext;
    private ObjectNameHelper objectNameHelper;
    private ExtensionDiscoverer extensionDiscoverer = new DefaultExtensionDiscoverer(new DefaultExtensionFactory(serviceRegistry), serviceRegistry);


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Extension> discoverExtensions(ClassLoader classLoader)
    {
        LOGGER.info("Starting discovery of extensions");

        List<Extension> discovered = extensionDiscoverer.discover(classLoader);
        LOGGER.info("Discovered {} extensions", discovered.size());

        for (Extension extension : discovered)
        {
            registerExtension(extension);
        }

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
    public <C> void registerConfigurationInstance(Extension extension, Configuration configuration, String configurationInstanceName, C configurationInstance)
    {
        ExtensionStateTracker extensionStateTracker = extensionRegistry.getExtensionState(extension);
        extensionStateTracker.registerConfigurationInstance(configuration, configurationInstanceName, configurationInstance);

        putInRegistryAndApplyLifecycle(configurationInstanceName, configurationInstance);
    }

    private void putInRegistryAndApplyLifecycle(String key, Object object)
    {
        try
        {
            muleContext.getRegistry().registerObject(objectNameHelper.getUniqueName(key), object);
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

    private void attemptToCreateImplicitConfigurationInstance(Extension extension, OperationContext operationContext)
    {
        Configuration implicitConfiguration = getImplicitConfiguration(extension);

        if (implicitConfiguration == null)
        {
            throw new IllegalStateException(String.format("Could not find a config for extension '%s' and none can be created automatically. Please define one", extension.getName()));
        }

        synchronized (implicitConfiguration)
        {
            //check that another thread didn't beat us to create the instance
            if (!extensionRegistry.getExtensionState(extension).getConfigurationInstanceProviders().isEmpty())
            {
                return;
            }

            final String instanceName = String.format("%s-%s", extension.getName(), implicitConfiguration.getName());
            ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(
                    instanceName,
                    extension,
                    implicitConfiguration,
                    buildImplicitConfigurationResolverSet(implicitConfiguration),
                    this);

            Object configurationInstance;
            try
            {
                configurationInstance = configurationObjectBuilder.build(asOperationContextAdapter(operationContext).getEvent());
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }

            registerConfigurationInstanceProvider(extension, instanceName, new StaticConfigurationInstanceProvider<>(configurationInstance));
        }
    }

    private ResolverSet buildImplicitConfigurationResolverSet(Configuration configuration)
    {
        ResolverSet resolverSet = new ResolverSet();
        for (Parameter parameter : configuration.getParameters())
        {
            Object defaultValue = parameter.getDefaultValue();
            if (defaultValue != null)
            {
                ValueResolver<Object> valueResolver;
                if (defaultValue instanceof String && muleContext.getExpressionManager().isExpression((String) defaultValue))
                {
                    valueResolver = new EvaluateAndTransformValueResolver<>((String) defaultValue, parameter.getType());
                }
                else
                {
                    valueResolver = new StaticValueResolver<>(defaultValue);
                }

                resolverSet.add(parameter, valueResolver);
            }
        }

        return resolverSet;
    }

    private Configuration getImplicitConfiguration(Extension extension)
    {
        for (Configuration configuration : extension.getConfigurations())
        {
            if (canBeUsedImplicitly(configuration))
            {
                return configuration;
            }
        }

        return null;
    }

    private boolean canBeUsedImplicitly(Configuration configuration)
    {
        for (Parameter parameter : configuration.getParameters())
        {
            if (parameter.isRequired() && parameter.getDefaultValue() == null)
            {
                return false;
            }
        }

        return true;
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

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        objectNameHelper = new ObjectNameHelper(muleContext);
    }

    void setExtensionsDiscoverer(ExtensionDiscoverer discoverer)
    {
        extensionDiscoverer = discoverer;
    }
}
