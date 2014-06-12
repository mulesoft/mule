/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.checkNullOrRepeatedNames;
import static org.mule.extensions.internal.MuleExtensionUtils.immutableList;
import org.mule.extensions.introspection.api.ExtensionConfiguration;
import org.mule.extensions.introspection.api.MuleExtensionParameter;

import java.util.List;

/**
 * Immutable implementation of {@link org.mule.extensions.introspection.api.ExtensionConfiguration}
 *
 * @since 1.0
 */
final class ImmutableExtensionConfiguration extends AbstractImmutableDescribed implements ExtensionConfiguration
{

    private final List<MuleExtensionParameter> parameters;

    protected ImmutableExtensionConfiguration(String name, String description, List<MuleExtensionParameter> parameters)
    {
        super(name, description);
        checkNullOrRepeatedNames(parameters, "parameters");
        this.parameters = immutableList(parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<MuleExtensionParameter> getParameters()
    {
        return parameters;
    }

}
