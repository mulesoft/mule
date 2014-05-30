/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionScope;
import org.mule.extensions.introspection.spi.MuleExtensionOperationGroupBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionScopeBuilder;

import java.util.LinkedList;
import java.util.List;

final class DefaultMuleExtensionScopeBuilder
        extends AbstractMuleExtensionOperationBuilder<MuleExtensionScope, MuleExtensionScopeBuilder>
        implements MuleExtensionScopeBuilder
{

    private List<MuleExtensionOperationGroupBuilder> groups = new LinkedList<MuleExtensionOperationGroupBuilder>();

    DefaultMuleExtensionScopeBuilder()
    {
    }

    @Override
    protected MuleExtensionScopeBuilder chain()
    {
        return this;
    }

    @Override
    public MuleExtensionScopeBuilder addOperationGroup(MuleExtensionOperationGroupBuilder group)
    {
        groups.add(group);
        return chain();
    }

    @Override
    public MuleExtensionScope build()
    {
        return new ImmutableMuleExtensionScope(name,
                                               description,
                                               ownerConfigurations,
                                               inputTypes,
                                               outputTypes,
                                               MuleExtensionUtils.build(parameters),
                                               MuleExtensionUtils.build(groups));
    }
}
