/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionOperationGroup;
import org.mule.extensions.introspection.spi.MuleExtensionOperationGroupBuilder;

final class DefaultMuleExtensionOperationGroupBuilder implements MuleExtensionOperationGroupBuilder
{

    private String name;
    private String description;
    private MuleExtensionOperationGroup.AllowedChildsType allowedChildsType;
    private int minOperations = 0;
    private int maxOperations = 0;

    DefaultMuleExtensionOperationGroupBuilder()
    {
    }

    @Override
    public MuleExtensionOperationGroupBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public MuleExtensionOperationGroupBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public MuleExtensionOperationGroupBuilder setAllowedChildsType(MuleExtensionOperationGroup.AllowedChildsType type)
    {
        this.allowedChildsType = type;
        return this;
    }

    @Override
    public MuleExtensionOperationGroupBuilder setMinOperations(int minOperations)
    {
        this.minOperations = minOperations;
        return this;
    }

    @Override
    public MuleExtensionOperationGroupBuilder setMaxOperations(int maxOperations)
    {
        this.maxOperations = maxOperations;
        return this;
    }

    @Override
    public MuleExtensionOperationGroup build()
    {
        return new ImmutableMuleExtensionOperationGroup(name,
                                                        description,
                                                        allowedChildsType,
                                                        minOperations,
                                                        maxOperations);
    }
}
