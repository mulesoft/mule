/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.CollectionUtils.immutableList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.fluent.OperationExecutorFactory;
import org.mule.extension.api.runtime.OperationExecutor;

import java.util.List;
import java.util.Map;

/**
 * Immutable concrete implementation of {@link OperationModel}
 *
 * @since 3.7.0
 */
final class ImmutableOperationModel extends AbstractImmutableModel implements OperationModel
{

    private final List<ParameterModel> parameterModels;
    private final OperationExecutorFactory executorFactory;

    /**
     * Creates a new instance with the given state
     *
     * @param name            the operation's name. Cannot be blank
     * @param description     the operation's descriptor
     * @param executorFactory a {@link OperationExecutorFactory}. Cannot be {@code null}
     * @param parameterModels a {@link List} with the operation's {@link ParameterModel parameterModels}
     * @param modelProperties A {@link Map} of custom properties which extend this model
     * @throws IllegalArgumentException if {@code name} is blank or {@code executorFactory} is {@code null}
     */
    ImmutableOperationModel(String name,
                            String description,
                            OperationExecutorFactory executorFactory,
                            List<ParameterModel> parameterModels,
                            Map<String, Object> modelProperties)
    {
        super(name, description, modelProperties);

        checkArgument(executorFactory != null, String.format("Operation '%s' cannot have a null executor factory", name));
        this.executorFactory = executorFactory;
        this.parameterModels = immutableList(parameterModels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterModel> getParameterModels()
    {
        return parameterModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationExecutor getExecutor()
    {
        return executorFactory.createExecutor();
    }
}
