/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.SPIServiceRegistry;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.config.i18n.MessageFactory;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.DelegatingOperationExecutor;
import org.mule.util.ObjectNameHelper;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExtensionManager}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionManager implements ExtensionManager, MuleContextAware, Initialisable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExtensionManager.class);

    private final ExtensionRegister register = new ExtensionRegister();
    private final ServiceRegistry serviceRegistry = new SPIServiceRegistry();

    private boolean initialised = false;
    private MuleContext muleContext;
    private ObjectNameHelper objectNameHelper;
    private ExtensionDiscoverer extensionDiscoverer = new DefaultExtensionDiscoverer(new DefaultExtensionFactory(serviceRegistry), serviceRegistry);

    @Override
    public void initialise() throws InitialisationException
    {
        for (Extension extension : getExtensions())
        {
            ExtensionStateTracker extensionState = register.getExtensionState(extension);
            for (ConfigurationInstanceWrapper<?> instanceWrapper : extensionState.getConfigurationInstances().values())
            {
                putInRegistryAndApplyLifecycle(instanceWrapper.getName(), instanceWrapper.getConfigurationInstance());
            }
        }

        initialised = true;
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
    public <C> void registerConfigurationInstance(Configuration configuration, String configurationInstanceName, C configurationInstance)
    {
        ExtensionStateTracker extensionStateTracker = register.getExtensionState(configuration);
        extensionStateTracker.registerConfigurationInstance(configurationInstanceName, configuration, configurationInstance);

        if (initialised)
        {
            putInRegistryAndApplyLifecycle(configurationInstanceName, configurationInstance);
        }
    }

    private <C> void putInRegistryAndApplyLifecycle(String configurationInstanceName, C configurationInstance)
    {
        try
        {
            muleContext.getRegistry().registerObject(objectNameHelper.getUniqueName(configurationInstanceName), configurationInstance);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> OperationExecutor getOperationExecutor(Operation operation, C configurationInstance)
    {
        ExtensionStateTracker extensionStateTracker = register.getExtensionState(operation);
        OperationExecutor executor;

        synchronized (configurationInstance)
        {
            executor = extensionStateTracker.getOperationExecutor(operation, configurationInstance);
            if (executor == null)
            {
                executor = createOperationExecutor(operation, configurationInstance);
                extensionStateTracker.registerOperationExecutor(operation, configurationInstance, executor);
            }
        }
        return executor;
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

    private <C> OperationExecutor createOperationExecutor(Operation operation, C configurationInstance)
    {
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
