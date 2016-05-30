/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.OperationContext;
import org.mule.runtime.extension.api.runtime.OperationExecutor;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

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
public final class OperationMessageProcessor extends ExtensionComponent implements MessageProcessor, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationMessageProcessor.class);

    private final RuntimeExtensionModel extensionModel;
    private final RuntimeOperationModel operationModel;
    private final ResolverSet resolverSet;
    private final String target;

    private ExecutionMediator executionMediator;
    private ReturnDelegate returnDelegate;
    private OperationExecutor operationExecutor;

    public OperationMessageProcessor(RuntimeExtensionModel extensionModel,
                                     RuntimeOperationModel operationModel,
                                     String configurationProviderName,
                                     String target,
                                     ResolverSet resolverSet,
                                     ExtensionManagerAdapter extensionManager)
    {
        super(extensionModel, operationModel, configurationProviderName, extensionManager);
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.resolverSet = resolverSet;
        this.target = target;
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

    private OperationContextAdapter createOperationContext(ConfigurationInstance<Object> configuration, MuleEvent event) throws MuleException
    {
        return new DefaultOperationContext(configuration, resolverSet.resolve(event), operationModel, event);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        returnDelegate = createReturnDelegate();
        operationExecutor = operationModel.getExecutor().createExecutor();
        executionMediator = new DefaultExecutionMediator(extensionModel, operationModel, connectionManager);
        initialiseIfNeeded(operationExecutor, true, muleContext);
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

    /**
     * Validates that the {@link #operationModel} is valid for the given {@code configurationProvider}
     *
     * @throws IllegalSourceException If the validation fails
     */
    @Override
    protected void validateOperationConfiguration(ConfigurationProvider<Object> configurationProvider)
    {
        RuntimeConfigurationModel configurationModel = configurationProvider.getModel();
        if (!configurationModel.getOperationModel(operationModel.getName()).isPresent() &&
            !configurationModel.getExtensionModel().getOperationModel(operationModel.getName()).isPresent())
        {
            throw new IllegalOperationException(String.format("Flow '%s' defines an usage of operation '%s' which points to configuration '%s'. " +
                                                              "The selected config does not support that operation.",
                                                              flowConstruct.getName(),
                                                              operationModel.getName(),
                                                              configurationProvider.getName()));
        }
    }
}
