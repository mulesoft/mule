/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.spi.MuleExtensionOperationBuilder;

final class DefaultMuleExtensionOperationBuilder extends
        AbstractMuleExtensionOperationBuilder<MuleExtensionOperation, MuleExtensionOperationBuilder>
        implements MuleExtensionOperationBuilder
{


    DefaultMuleExtensionOperationBuilder()
    {
    }

    @Override
    public MuleExtensionOperation build()
    {
        return new ImmutableMuleExtensionOperation(name,
                                                   description,
                                                   ownerConfigurations,
                                                   inputTypes,
                                                   outputTypes,
                                                   MuleExtensionUtils.build(parameters));
    }

    @Override
    protected MuleExtensionOperationBuilder chain()
    {
        return this;
    }
}
