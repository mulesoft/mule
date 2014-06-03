/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.toMap;
import org.mule.extensions.introspection.api.MuleExtensionOperationGroup;
import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.extensions.introspection.api.MuleExtensionScope;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable implementation of {@link org.mule.extensions.introspection.api.MuleExtensionScope}
 */
final class ImmutableMuleExtensionScope extends AbstractMuleExtensionOperation implements MuleExtensionScope
{

    private final Map<String, MuleExtensionOperationGroup> groups;

    ImmutableMuleExtensionScope(String name,
                                String description,
                                Set<String> ownerConfigurations,
                                List<Class<?>> inputTypes,
                                List<Class<?>> outputTypes,
                                List<MuleExtensionParameter> parameters,
                                List<MuleExtensionOperationGroup> groups)
    {
        super(name, description, ownerConfigurations, inputTypes, outputTypes, parameters);
        this.groups = toMap(groups);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MuleExtensionOperationGroup> getGroups()
    {
        return ImmutableList.copyOf(groups.values());
    }
}
