/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link OperationContextAdapter} which
 * adds additional information which is relevant to this implementation
 * of the extensions-api, even though it's not part of the API itself
 *
 * @since 3.7.0
 */
public class DefaultOperationContext implements OperationContextAdapter
{

    private final String configurationInstanceProviderName;
    private final Extension extension;
    private final Operation operation;
    private final Map<String, Object> parameters;
    private final MuleEvent event;
    private final ExtensionManagerAdapter extensionManager;

    private Object configurationInstance;

    /**
     * Creates a new instance with the given state
     *
     * @param operation        the {@link Operation} that will be executed
     * @param parameters       the parameters that the operation will use
     * @param event            the current {@link MuleEvent}
     * @param extensionManager the {@link ExtensionManagerAdapter} on which the {@link Extension} that owns the {@link Operation} is registered
     */
    public DefaultOperationContext(Extension extension,
                                   Operation operation,
                                   String configurationInstanceProviderName,
                                   ResolverSetResult parameters,
                                   MuleEvent event,
                                   ExtensionManagerAdapter extensionManager)
    {
        this.extension = extension;
        this.operation = operation;
        this.configurationInstanceProviderName = configurationInstanceProviderName;
        this.event = event;
        this.extensionManager = extensionManager;

        Map<Parameter, Object> parameterMap = parameters.asMap();
        this.parameters = new HashMap<>(parameterMap.size());
        for (Map.Entry<Parameter, Object> parameter : parameterMap.entrySet())
        {
            this.parameters.put(parameter.getKey().getName(), parameter.getValue());
        }
    }

    @Override
    public <C> C getConfigurationInstance()
    {
        if (configurationInstance == null)
        {
            configurationInstance = StringUtils.isBlank(configurationInstanceProviderName)
                                    ? extensionManager.getConfigurationInstance(extension, this)
                                    : extensionManager.getConfigurationInstance(extension, configurationInstanceProviderName, this);
        }

        return (C) configurationInstance;
    }

    @Override
    public Operation getOperation()
    {
        return operation;
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
