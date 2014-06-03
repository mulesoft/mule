/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionParameter;

import java.util.List;
import java.util.Set;

/**
 * Immutable concrete implementation of {@link org.mule.extensions.introspection.api.MuleExtensionOperation}
 *
 * @since 1.0
 */
final class ImmutableMuleExtensionOperation extends AbstractMuleExtensionOperation
{

    ImmutableMuleExtensionOperation(String name,
                                    String description,
                                    Set<String> ownerConfigurations,
                                    List<Class<?>> inputTypes,
                                    List<Class<?>> outputTypes,
                                    List<MuleExtensionParameter> parameters)
    {
        super(name, description, ownerConfigurations, inputTypes, outputTypes, parameters);
    }
}
