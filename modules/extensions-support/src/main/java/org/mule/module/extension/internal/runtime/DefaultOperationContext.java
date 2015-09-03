/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.extension.runtime.event.OperationFailedSignal;
import org.mule.extension.runtime.event.OperationSuccessfulSignal;
import org.mule.module.extension.internal.config.DeclaredConfiguration;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.util.StringUtils;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Default implementation of {@link OperationContextAdapter} which
 * adds additional information which is relevant to this implementation
 * of the extensions-api, even though it's not part of the API itself
 *
 * @since 3.7.0
 */
public class DefaultOperationContext implements OperationContextAdapter
{

    private final String configurationProviderName;
    private final ExtensionModel extensionModel;
    private final OperationModel operationModel;
    private final Map<String, Object> parameters;
    private final MuleEvent event;
    private final ExtensionManagerAdapter extensionManager;
    private final EventBus eventBus = new EventBus();

    private Object configuration;
    private ConfigurationModel configurationModel;

    /**
     * Creates a new instance with the given state
     *
     * @param operationModel   the {@link OperationModel} that will be executed
     * @param parameters       the parameters that the operation will use
     * @param event            the current {@link MuleEvent}
     * @param extensionManager the {@link ExtensionManagerAdapter} on which the {@link ExtensionModel} that owns the {@link OperationModel} is registered
     * @oaram extensionModel the {@link ExtensionModel} that owns the {@code operationModel}
     */
    public DefaultOperationContext(ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   String configurationProviderName,
                                   ResolverSetResult parameters,
                                   MuleEvent event,
                                   ExtensionManagerAdapter extensionManager)
    {
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.configurationProviderName = configurationProviderName;
        this.event = event;
        this.extensionManager = extensionManager;

        Map<ParameterModel, Object> parameterMap = parameters.asMap();
        this.parameters = new HashMap<>(parameterMap.size());
        for (Map.Entry<ParameterModel, Object> parameter : parameterMap.entrySet())
        {
            this.parameters.put(parameter.getKey().getName(), parameter.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> C getConfiguration()
    {
        resolveConfig();
        return (C) configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationModel getConfigurationModel()
    {
        resolveConfig();
        return configurationModel;
    }

    private void resolveConfig()
    {
        if (configuration == null || configurationModel == null)
        {
            DeclaredConfiguration<Object> declaredConfiguration = StringUtils.isBlank(configurationProviderName)
                                                                  ? extensionManager.getConfiguration(extensionModel, this)
                                                                  : extensionManager.getConfiguration(extensionModel, configurationProviderName, this);

            configuration = declaredConfiguration.getValue();
            configurationModel = declaredConfiguration.getModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifySuccessfulOperation(Object result)
    {
        eventBus.post(new OperationSuccessfulSignal(result));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyFailedOperation(Exception exception)
    {
        eventBus.post(new OperationFailedSignal(exception));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOperationSuccessful(Consumer<OperationSuccessfulSignal> handler)
    {
        eventBus.register(new Object()
        {
            @Subscribe
            public void handle(OperationSuccessfulSignal event)
            {
                handler.accept(event);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOperationFailed(Consumer<OperationFailedSignal> handler)
    {
        eventBus.register(new Object()
        {
            @Subscribe
            public void handle(OperationFailedSignal event)
            {
                handler.accept(event);
            }
        });
    }

    @Override
    public OperationModel getOperationModel()
    {
        return operationModel;
    }

    @Override
    public Object getParameterValue(String parameterName)
    {
        return parameters.get(parameterName);
    }

    @Override
    public MuleEvent getEvent()
    {
        return event;
    }
}
