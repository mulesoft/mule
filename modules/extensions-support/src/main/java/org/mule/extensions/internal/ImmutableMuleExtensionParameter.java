/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.util.Preconditions;

/**
 * Immutable implementation of {@link org.mule.extensions.introspection.api.MuleExtensionParameter}
 *
 * @since 1.0
 */
final class ImmutableMuleExtensionParameter extends AbstractImmutableDescribed implements MuleExtensionParameter
{

    private final Class<?> type;
    private final boolean required;
    private final boolean dynamic;
    private final Object defaultValue;

    protected ImmutableMuleExtensionParameter(String name,
                                              String description,
                                              Class<?> type,
                                              boolean required,
                                              boolean dynamic,
                                              Object defaultValue)
    {
        super(name, description);

        Preconditions.checkState(type != null, "Parameters must have a type");
        if (defaultValue != null)
        {
            Preconditions.checkState(type.isInstance(defaultValue), String.format("Parameter of type '%s' cannot have a default value of type '%s'",
                                                                                  type.getName(),
                                                                                  defaultValue.getClass().getCanonicalName()));
        }

        this.type = type;
        this.required = required;
        this.dynamic = dynamic;
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType()
    {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequired()
    {
        return required;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic()
    {
        return dynamic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue()
    {
        return defaultValue;
    }

}
