/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.CollectionUtils.immutableList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.declaration.fluent.OperationExecutorFactory;
import org.mule.extension.runtime.OperationExecutor;

import java.util.List;
import java.util.Set;

/**
 * Immutable concrete implementation of {@link Operation}
 *
 * @since 3.7.0
 */
final class ImmutableOperation extends AbstractCapableDescribed implements Operation
{

    private final List<Parameter> parameters;
    private final OperationExecutorFactory executorFactory;

    /**
     * Creates a new instance with the given state
     *
     * @param name            the operation's name. Cannot be blank
     * @param description     the operation's descriptor
     * @param executorFactory a {@link OperationExecutorFactory}. Cannot be {@code null}
     * @param parameters      a {@link List} with the operation's {@link Parameter}s
     * @param capabilities    a {@link Set} with the operation's capabilities
     */
    ImmutableOperation(String name,
                       String description,
                       OperationExecutorFactory executorFactory,
                       List<Parameter> parameters,
                       Set<Object> capabilities)
    {
        super(name, description, capabilities);

        checkArgument(executorFactory != null, String.format("Operation '%s' cannot have a null executor factory", name));
        this.executorFactory = executorFactory;
        this.parameters = immutableList(parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Parameter> getParameters()
    {
        return parameters;
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
