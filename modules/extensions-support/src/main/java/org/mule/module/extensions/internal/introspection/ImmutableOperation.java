/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.module.extensions.internal.util.MuleExtensionUtils.immutableList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.OperationImplementation;
import org.mule.extensions.introspection.Parameter;

import java.util.List;
import java.util.Set;

/**
 * Immutable concrete implementation of {@link Operation}
 *
 * @since 3.7.0
 */
final class ImmutableOperation extends AbstractImmutableCapableDescribed implements Operation
{

    private final List<Parameter> parameters;
    private final OperationImplementation implementation;

    ImmutableOperation(String name,
                       String description,
                       OperationImplementation implementation,
                       List<Parameter> parameters,
                       Set<Object> capabilities)
    {
        super(name, description, capabilities);

        checkArgument(implementation != null, String.format("Operation %s cannot have a null implementation", name));
        this.implementation = implementation;
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
    public OperationImplementation getImplementation()
    {
        return implementation;
    }
}
