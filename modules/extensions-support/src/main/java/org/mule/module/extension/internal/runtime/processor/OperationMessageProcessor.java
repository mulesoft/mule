/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.metadata.DefaultMetadataContext;
import org.mule.api.metadata.MetadataAware;
import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.resolving.FailureCode;
import org.mule.api.metadata.resolving.MetadataResult;
import org.mule.api.processor.MessageProcessor;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.RuntimeConfigurationModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.RuntimeOperationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.internal.connection.ConnectionManagerAdapter;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.metadata.MetadataMediator;
import org.mule.module.extension.internal.runtime.DefaultExecutionMediator;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.ExecutionMediator;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.util.StringUtils;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * <p>
 * It obtains a configuration instance, evaluate all the operation parameters
 * and executes a {@link RuntimeOperationModel} by using a {@link #operationExecutor}. This message processor is capable
 * of serving the execution of any {@link OperationModel} of any {@link RuntimeExtensionModel}.
 * <p>
 * A {@link #operationExecutor} is obtained by invoking {@link RuntimeOperationModel#getExecutor()}. That instance
 * will be use to serve all invokations of {@link #process(MuleEvent)} on {@code this} instance but
 * will not be shared with other instances of {@link OperationMessageProcessor}. All the {@link Lifecycle}
 * events that {@code this} instance receives will be propagated to the {@link #operationExecutor}.
 * <p>
 * The {@link #operationExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor, MuleContextAware, Lifecycle, FlowConstructAware, MetadataAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationMessageProcessor.class);

    private final RuntimeExtensionModel extensionModel;
    private final String configurationProviderName;
    private final RuntimeOperationModel operationModel;
    private final ResolverSet resolverSet;
    private final ExtensionManagerAdapter extensionManager;
    private final String target;

    private ExecutionMediator executionMediator;
    private ReturnDelegate returnDelegate;
    private MuleContext muleContext;
    private OperationExecutor operationExecutor;
    private MetadataMediator metadataMediator;

    private Optional<ConfigurationProvider<Object>> configurationProvider;

    @Inject
    private ConnectionManagerAdapter connectionManager;
    private FlowConstruct flowConstruct;

    public OperationMessageProcessor(RuntimeExtensionModel extensionModel,
                                     RuntimeOperationModel operationModel,
                                     String configurationProviderName,
                                     String target,
                                     ResolverSet resolverSet,
                                     ExtensionManagerAdapter extensionManager)
    {
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.configurationProviderName = configurationProviderName;
        this.resolverSet = resolverSet;
        this.extensionManager = extensionManager;
        this.target = target;
        this.metadataMediator = new MetadataMediator(operationModel);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ConfigurationInstance<Object> configuration = getConfiguration(event);
        OperationContextAdapter operationContext = createOperationContext(configuration, event);
        Object result = executeOperation(operationContext, event);

        return returnDelegate.asReturnValue(result, operationContext);
    }

    private Object executeOperation(OperationContext operationContext, MuleEvent event) throws MuleException
    {
        try
        {
            return executionMediator.execute(operationExecutor, operationContext);
        }
        catch (Throwable e)
        {
            throw new MessagingException(createStaticMessage(e.getMessage()), event, e, this);
        }
    }

    private ConfigurationInstance<Object> getConfiguration(MuleEvent event)
    {
        return configurationProvider
                .map(provider -> provider.get(event))
                .orElseGet(() -> {
                    if (StringUtils.isBlank(configurationProviderName))
                    {
                        return extensionManager.getConfiguration(extensionModel, event);
                    }
                    return extensionManager.getConfiguration(configurationProviderName, event);
                });
    }

    private OperationContextAdapter createOperationContext(ConfigurationInstance<Object> configuration, MuleEvent event) throws MuleException
    {
        return new DefaultOperationContext(configuration, resolverSet.resolve(event), operationModel, event);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        returnDelegate = createReturnDelegate();
        operationExecutor = operationModel.getExecutor().createExecutor();
        executionMediator = new DefaultExecutionMediator(extensionModel, operationModel, connectionManager);
        configurationProvider = getConfigurationProvider();
        initialiseIfNeeded(operationExecutor, true, muleContext);
    }

    private Optional<ConfigurationProvider<Object>> getConfigurationProvider()
    {
        Optional<ConfigurationProvider<Object>> provider = StringUtils.isBlank(configurationProviderName)
                                                           ? extensionManager.getConfigurationProvider(extensionModel)
                                                           : extensionManager.getConfigurationProvider(configurationProviderName);

        if (provider.isPresent())
        {
            RuntimeConfigurationModel configurationModel = provider.get().getModel();
            if (!configurationModel.getOperationModel(operationModel.getName()).isPresent() &&
                !configurationModel.getExtensionModel().getOperationModel(operationModel.getName()).isPresent())
            {
                throw new IllegalOperationException(String.format("Flow '%s' defines an usage of operation '%s' which points to configuration '%s'. " +
                                                                  "The selected config does not support that operation.",
                                                                  flowConstruct.getName(),
                                                                  operationModel.getName(),
                                                                  provider.get().getName()));
            }
        }

        return provider;
    }

    private ReturnDelegate createReturnDelegate()
    {
        if (isVoid(operationModel))
        {
            return VoidReturnDelegate.INSTANCE;
        }

        return StringUtils.isBlank(target) ? new ValueReturnDelegate(muleContext) : new TargetReturnDelegate(target, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        startIfNeeded(operationExecutor);
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(operationExecutor);
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(operationExecutor, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
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
    public MetadataResult<OperationMetadataDescriptor> getMetadata() throws MetadataResolvingException
    {
        return metadataMediator.getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<OperationMetadataDescriptor> getMetadata(MetadataKey key) throws MetadataResolvingException
    {
        return metadataMediator.getMetadata(getMetadataContext(), key);
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
        return new DefaultMetadataContext(configuration, connectionManager);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
