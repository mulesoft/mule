/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link OperationContext} which
 * adds additional information which is relevant to this implementation
 * of the extensions-api, even though it's not part of the API itself
 *
 * @since 3.7.0
 */
public final class DefaultOperationContext implements OperationContext
{

    private final Map<Parameter, Object> parameters;
    private final Map<String, Object> parametersByName;

    /**
     * The current {@link MuleEvent}
     */
    private final MuleEvent event;

    public DefaultOperationContext(ResolverSetResult parameters, MuleEvent event)
    {
        this.parameters = parameters.asMap();
        parametersByName = new HashMap<>(this.parameters.size());
        for (Map.Entry<Parameter, Object> parameter : this.parameters.entrySet())
        {
            parametersByName.put(parameter.getKey().getName(), parameter.getValue());
        }

        this.event = event;
    }

    @Override
    public Map<Parameter, Object> getParameters()
    {
        return parameters;
    }

    @Override
    public Object getParameterValue(Parameter parameter)
    {
        return parameters.get(parameter);
    }

    @Override
    public Object getParameterValue(String parameterName)
    {
        return parametersByName.get(parameterName);
    }

    public MuleEvent getEvent()
    {
        return event;
    }
}
