/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import org.mule.api.MuleEvent;
import org.mule.extensions.introspection.OperationContext;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extensions.internal.util.ValueSetter;

import java.util.List;
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

    /**
     * The configuration instance for this execution
     */
    private final Object configurationInstance;

    /**
     * the values for each parameter
     */
    private final ResolverSetResult parameters;

    /**
     * The current {@link MuleEvent}
     */
    private final MuleEvent event;

    /**
     *  A list of {@link ValueSetter} to resolve parameter groups
     */
    private final List<ValueSetter> groupValueSetters;

    public DefaultOperationContext(Object configurationInstance, ResolverSetResult parameters, MuleEvent event, List<ValueSetter> groupValueSetters)
    {
        this.configurationInstance = configurationInstance;
        this.parameters = parameters;
        this.event = event;
        this.groupValueSetters = groupValueSetters;
    }

    @Override
    public Object getConfigurationInstance()
    {
        return configurationInstance;
    }

    @Override
    public Map<Parameter, Object> getParametersValues()
    {
        return parameters.asMap();
    }

    ResolverSetResult getParameters()
    {
        return parameters;
    }

    MuleEvent getEvent()
    {
        return event;
    }

    List<ValueSetter> getGroupValueSetters()
    {
        return groupValueSetters;
    }
}
