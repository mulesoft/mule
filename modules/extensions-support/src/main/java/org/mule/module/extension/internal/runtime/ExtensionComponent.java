/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.internal.metadata.DefaultMetadataContext;
import org.mule.api.metadata.MetadataAware;
import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.internal.metadata.MuleMetadataManager;
import org.mule.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.api.metadata.resolving.FailureCode;
import org.mule.api.metadata.resolving.MetadataResult;
import org.mule.extension.api.introspection.RuntimeComponentModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.internal.connection.ConnectionManagerAdapter;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.metadata.MetadataMediator;
import org.mule.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.module.extension.internal.runtime.processor.OperationMessageProcessor;
import org.mule.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.util.StringUtils;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Class that groups all the common behaviour between different extension's components, like {@link OperationMessageProcessor}
 * and {@link ExtensionMessageSource}.
 * <p>
 * Provides capabilities of Metadata resolution and configuration validation.
 *
 * @since 4.0
 */
public abstract class ExtensionComponent implements MuleContextAware, MetadataAware, FlowConstructAware, Initialisable
{

    private final RuntimeExtensionModel extensionModel;
    private final String configurationProviderName;
    protected final ExtensionManagerAdapter extensionManager;

    private MetadataMediator metadataMediator;
    protected FlowConstruct flowConstruct;
    protected MuleContext muleContext;

    @Inject
    protected ConnectionManagerAdapter connectionManager;

    @Inject
    private MuleMetadataManager metadataManager;

    protected ExtensionComponent(RuntimeExtensionModel extensionModel, RuntimeComponentModel componentModel, String configurationProviderName, ExtensionManagerAdapter extensionManager)
    {
        this.extensionModel = extensionModel;
        this.configurationProviderName = configurationProviderName;
        this.extensionManager = extensionManager;
        this.metadataMediator = new MetadataMediator(componentModel);
    }

    /**
     * Makes sure that the operation is valid by invoking {@link #validateOperationConfiguration(ConfigurationProvider)}
     * and then delegates on {@link #doInitialise()} for custom initialisation
     *
     * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
     */
    @Override
    public final void initialise() throws InitialisationException
    {
        Optional<ConfigurationProvider<Object>> provider = getConfigurationProvider();

        if (provider.isPresent())
        {
            validateOperationConfiguration(provider.get());
        }

        doInitialise();
    }

    /**
     * Implementors will use this method to perform their own initialisation
     * logic
     *
     * @throws InitialisationException if a fatal error occurs causing the Mule instance to shutdown
     */
    protected abstract void doInitialise() throws InitialisationException;

    /**
     * Validates that the configuration returned by the {@code configurationProvider}
     * is compatible with the associated {@link RuntimeComponentModel}
     *
     * @param configurationProvider
     */
    protected abstract void validateOperationConfiguration(ConfigurationProvider<Object> configurationProvider);

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<List<MetadataKey>> getMetadataKeys() throws MetadataResolvingException
    {
        return metadataMediator.getMetadataKeys(getMetadataContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<ComponentMetadataDescriptor> getMetadata() throws MetadataResolvingException
    {
        return metadataMediator.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataKey key) throws MetadataResolvingException
    {
        return metadataMediator.getMetadata(getMetadataContext(), key);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    private MetadataContext getMetadataContext() throws MetadataResolvingException
    {
        //TODO MULE-9530: Improve Config retrieval for Metadata resolution
        if (!StringUtils.isBlank(configurationProviderName) &&
            muleContext.getRegistry().get(configurationProviderName) instanceof DynamicConfigurationProvider)
        {
            throw new MetadataResolvingException("Configuration used for Metadata fetch cannot be dynamic", FailureCode.INVALID_CONFIGURATION);
        }

        ConfigurationInstance<Object> configuration = getConfiguration(getInitialiserEvent(muleContext));
        String cacheId = configuration.getName();

        return new DefaultMetadataContext(configuration, connectionManager, metadataManager.getMetadataCache(cacheId));
    }

    /**
     * @param event a {@link MuleEvent}
     * @return a configuration instance for the current component with a given {@link MuleEvent}
     */
    protected ConfigurationInstance<Object> getConfiguration(MuleEvent event)
    {
        return getConfigurationProvider()
                .map(provider -> provider.get(event))
                .orElseGet(() -> {
                    if (StringUtils.isBlank(configurationProviderName))
                    {
                        return extensionManager.getConfiguration(extensionModel, event);
                    }
                    return extensionManager.getConfiguration(configurationProviderName, event);
                });
    }

    private Optional<ConfigurationProvider<Object>> getConfigurationProvider()
    {
        Optional<ConfigurationProvider<Object>> provider = StringUtils.isBlank(configurationProviderName)
                                                           ? extensionManager.getConfigurationProvider(extensionModel)
                                                           : extensionManager.getConfigurationProvider(configurationProviderName);
        return provider;
    }

}
