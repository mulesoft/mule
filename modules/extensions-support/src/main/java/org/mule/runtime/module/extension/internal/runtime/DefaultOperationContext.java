/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

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

    private final ConfigurationInstance<?> configuration;
    private final Map<String, Object> parameters;
    private final Map<String, Object> variables = new HashMap<>();
    private final RuntimeOperationModel operationModel;
    private final MuleEvent event;

    /**
     * Creates a new instance with the given state
     *
     * @param configuration  the {@link ConfigurationInstance} that the operation will use
     * @param parameters     the parameters that the operation will use
     * @param operationModel a {@link RuntimeOperationModel} for the operation being executed
     * @param event          the current {@link MuleEvent}
     */
    public DefaultOperationContext(ConfigurationInstance<Object> configuration, ResolverSetResult parameters, RuntimeOperationModel operationModel, MuleEvent event)
    {
        this.configuration = configuration;
        this.event = event;
        this.operationModel = operationModel;

        Map<ParameterModel, Object> parameterMap = parameters.asMap();
        this.parameters = new HashMap<>(parameterMap.size());
        parameters.asMap().entrySet().forEach(parameter -> setParameter(parameter));
    }

    private void setParameter(Map.Entry<ParameterModel, Object> parameter)
    {
        this.parameters.put(parameter.getKey().getName(), parameter.getValue());
    }

    /**
     * {@inheritDoc}
     */
    public <C> ConfigurationInstance<C> getConfiguration()
    {
        return (ConfigurationInstance<C>) configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getParameter(String parameterName)
    {
        return (T) parameters.get(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getTypeSafeParameter(String parameterName, Class<? extends T> expectedType)
    {
        Object value = getParameter(parameterName);
        if (value == null)
        {
            return null;
        }

        if (!expectedType.isInstance(value))
        {
            throw new IllegalArgumentException(String.format("'%s' was expected to be of type '%s' but type '%s' was found instead",
                                                             parameterName, expectedType.getName(), value.getClass().getName()));
        }

        return (T) value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getVariable(String key)
    {
        return (T) variables.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object setVariable(String key, Object value)
    {
        checkArgument(key != null, "null keys are not allowed");
        checkArgument(value != null, "null values are not allowed");
        return variables.put(key, value);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T removeVariable(String key)
    {
        checkArgument(key != null, "null keys are not allowed");
        return (T) variables.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleEvent getEvent()
    {
        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuntimeOperationModel getOperationModel()
    {
        return operationModel;
    }
}
