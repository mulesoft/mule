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
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.registry.SpiServiceRegistry;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.config.i18n.MessageFactory;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.DelegatingOperationExecutor;
import org.mule.module.extension.internal.runtime.StaticConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.resolver.EvaluateAndTransformValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.util.ObjectNameHelper;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManagerAdapter}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManagerAdapter, MuleContextAware, Initialisable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

    private final ExtensionRegister register = new ExtensionRegister();
    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();

    private MuleContext muleContext;
    private ObjectNameHelper objectNameHelper;
    private ExtensionDiscoverer extensionDiscoverer = new DefaultExtensionDiscoverer(new DefaultExtensionFactory(serviceRegistry), serviceRegistry);

    /**
     * Searches the mule registry for instances of {@link ConfigurationInstanceProvider}
     * and registers them through the {@link #registerConfigurationInstanceProvider(String, ConfigurationInstanceProvider)}
     * method
     */
    @Override
    public void initialise() throws InitialisationException
    {
        for (Map.Entry<String, ConfigurationInstanceProvider> instanceProviderEntry : muleContext.getRegistry().lookupByType(ConfigurationInstanceProvider.class).entrySet())
        {
            ConfigurationInstanceProvider<?> instanceProvider = instanceProviderEntry.getValue();
            registerConfigurationInstanceProvider(instanceProviderEntry.getKey(), instanceProvider);
        }
    }

    @Override
    public List<Extension> discoverExtensions(ClassLoader classLoader)
    {
        LOGGER.info("Starting discovery of extensions");

        List<Extension> discovered = extensionDiscoverer.discover(classLoader);
        LOGGER.info("Discovered {} extensions", discovered.size());

        ImmutableList.Builder<Extension> accepted = ImmutableList.builder();

        for (Extension extension : discovered)
        {
            if (registerExtension(extension))
            {
                accepted.add(extension);
            }
        }

        return accepted.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerExtension(Extension extension)
    {
        LOGGER.info("Registering extension {} (version {})", extension.getName(), extension.getVersion());
        final String extensionName = extension.getName();

        if (register.containsExtension(extensionName))
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
    public <C> void registerConfigurationInstanceProvider(String providerName, ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        Configuration configuration = configurationInstanceProvider.getConfiguration();
        ExtensionStateTracker extensionStateTracker = register.getExtensionState(configuration);
        extensionStateTracker.registerConfigurationInstanceProvider(configuration, providerName, configurationInstanceProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfigurationInstance(final ConfigurationInstanceProvider<C> configurationInstanceProvider, OperationContext operationContext)
    {
        return configurationInstanceProvider.get(operationContext, new ConfigurationInstanceRegistrationCallback()
        {
            @Override
            public <C> void registerNewConfigurationInstance(ConfigurationInstanceProvider<C> configurationInstanceProvider, C configurationInstance)
            {
                registerConfigurationInstance(configurationInstanceProvider.getConfiguration(),
                                              configurationInstanceProvider.getName(),
                                              configurationInstance);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationExecutor getOperationExecutor(OperationContext operationContext)
    {
        Extension extension = register.getExtension(operationContext.getOperation());
        Map<String, ConfigurationInstanceProvider> providers = register.getConfigurationInstanceProviders(extension);

        int matches = providers.size();

        if (matches == 1)
        {
            ConfigurationInstanceProvider<Object> provider = providers.values().iterator().next();
            return getOperationExecutor(provider, operationContext);
        }
        else if (matches > 1)
        {
            throw new IllegalStateException(String.format("No config-ref was specified for operation %s of extension %s, but %d are registered. Please specify which to use",
                                                          operationContext.getOperation().getName(), extension.getName(), matches));
        }
        else
        {
            attemptToCreateImplicitConfigurationInstance(extension, operationContext);
            return getOperationExecutor(operationContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationExecutor getOperationExecutor(final String configurationInstanceProviderName, OperationContext operationContext)
    {
        ConfigurationInstanceProvider<Object> configurationInstanceProvider = getConfigurationInstanceProvider(configurationInstanceProviderName);
        return getOperationExecutor(configurationInstanceProvider, operationContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Extension> getExtensions()
    {
        return register.getExtensions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> Set<Extension> getExtensionsCapableOf(Class<C> capabilityType)
    {
        checkArgument(capabilityType != null, "capability type cannot be null");
        return register.getExtensionsCapableOf(capabilityType);
    }

    private <C> void registerConfigurationInstance(Configuration configuration, String configurationInstanceName, C configurationInstance)
    {
        ExtensionStateTracker extensionStateTracker = register.getExtensionState(configuration);
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

    private OperationExecutor getOperationExecutor(ConfigurationInstanceProvider<Object> configurationInstanceProvider, OperationContext operationContext)
    {
        Object configurationInstance = getConfigurationInstance(configurationInstanceProvider, operationContext);
        OperationExecutor executor;

        synchronized (configurationInstance)
        {
            ExtensionStateTracker extensionStateTracker = register.getExtensionState(configurationInstanceProvider.getConfiguration());
            executor = extensionStateTracker.getOperationExecutor(configurationInstanceProvider.getConfiguration(), configurationInstance, operationContext);
            if (executor == null)
            {
                executor = createOperationExecutor(configurationInstance, operationContext);
                extensionStateTracker.registerOperationExecutor(configurationInstanceProvider.getConfiguration(),
                                                                operationContext.getOperation(),
                                                                configurationInstance,
                                                                executor);
            }
        }

        return executor;
    }

    private ConfigurationInstanceProvider<Object> getConfigurationInstanceProvider(String configurationInstanceProviderName)
    {
        ConfigurationInstanceProvider<Object> configurationInstanceProvider = register.getConfigurationInstanceProviders().get(configurationInstanceProviderName);
        if (configurationInstanceProvider == null)
        {
            throw new IllegalArgumentException("There's no registered ConfigurationInstanceProvider under name" + configurationInstanceProviderName);
        }
        return configurationInstanceProvider;
    }

    private void attemptToCreateImplicitConfigurationInstance(Extension extension, OperationContext operationContext)
    {
        Configuration implicitConfiguration = getImplicitConfiguration(extension);

        if (implicitConfiguration == null)
        {
            throw new IllegalStateException(String.format("Could not find a config for extension %s and none can be created automatically. Please define one", extension.getName()));
        }

        synchronized (implicitConfiguration)
        {
            //check that another thread didn't beat us to create the instance
            if (!register.getConfigurationInstanceProviders(extension).isEmpty())
            {
                return;
            }

            ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(implicitConfiguration, buildImplicitConfigurationResolverSet(implicitConfiguration));

            Object configurationInstance;
            try
            {
                configurationInstance = configurationObjectBuilder.build(asOperationContextAdapter(operationContext).getEvent());
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }

            final String instanceName = objectNameHelper.getUniqueName(String.format("%s-%s", extension.getName(), implicitConfiguration.getName()));
            registerConfigurationInstanceProvider(instanceName,
                                                  new StaticConfigurationInstanceProvider<>(instanceName, implicitConfiguration, configurationInstance));
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
        Extension actual = register.getExtension(extensionName);
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
        register.registerExtension(extensionName, extension);
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

    private <C> OperationExecutor createOperationExecutor(C configurationInstance, OperationContext operationContext)
    {
        Operation operation = operationContext.getOperation();
        OperationExecutor executor;
        executor = operation.getExecutor(configurationInstance);
        if (executor instanceof DelegatingOperationExecutor)
        {
            Extension extension = register.getExtension(operation);
            String executorName = objectNameHelper.getUniqueName(String.format("%s_executor_%s", extension.getName(), operation.getName()));
            try
            {
                muleContext.getRegistry().registerObject(executorName, ((DelegatingOperationExecutor<Object>) executor).getExecutorDelegate());
            }
            catch (RegistrationException e)
            {
                throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not create new executor for operation"), e);
            }
        }

        return executor;
    }

    protected void setExtensionsDiscoverer(ExtensionDiscoverer discoverer)
    {
        extensionDiscoverer = discoverer;
    }
}
