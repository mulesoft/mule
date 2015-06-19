/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

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

    private final Operation operation;
    private final Map<String, Object> parameters;
    private final MuleEvent event;

    public DefaultOperationContext(Operation operation, ResolverSetResult parameters, MuleEvent event)
    {
        this.operation = operation;
        this.event = event;

        Map<Parameter, Object> parameterMap = parameters.asMap();
        this.parameters = new HashMap<>(parameterMap.size());
        for (Map.Entry<Parameter, Object> parameter : parameterMap.entrySet())
        {
            this.parameters.put(parameter.getKey().getName(), parameter.getValue());
        }
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
