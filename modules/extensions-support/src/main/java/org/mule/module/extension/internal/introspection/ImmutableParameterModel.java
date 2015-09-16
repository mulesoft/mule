/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.ParameterModel;

import java.util.Map;

/**
 * Immutable implementation of {@link ParameterModel}
 *
 * @since 3.7.0
 */
final class ImmutableParameterModel extends AbstractImmutableModel implements ParameterModel
{

    private final DataType type;
    private final boolean required;
    private final boolean dynamic;
    private final Object defaultValue;

    /**
     * Creates a new instance with the given state
     *
     * @param name            the parameter's name. Cannot be blank and cannot be one of the values in {@link #RESERVED_NAMES}
     * @param description     the parameter's description
     * @param type            the parameter's {@link DataType}. Cannot be {@code null}
     * @param required        whether this parameter is required or not
     * @param dynamic         whether this parameter is dynamic or not
     * @param defaultValue    this parameter's default value
     * @param modelProperties A {@link Map} of custom properties which extend this model
     * @throws IllegalArgumentException if {@code required} is {@code true} and {@code defaultValue} is not {@code null} at the same time
     */
    protected ImmutableParameterModel(String name,
                                      String description,
                                      DataType type,
                                      boolean required,
                                      boolean dynamic,
                                      Object defaultValue,
                                      Map<String, Object> modelProperties)
    {
        super(name, description, modelProperties);

        if (RESERVED_NAMES.contains(name))
        {
            throw new IllegalArgumentException(
                    String.format("Extension parameter cannot have the name ['%s'] since it's a reserved one", name));
        }

        checkArgument(type != null, "Extension parameters must have a type");

        if (required && defaultValue != null)
        {
            throw new IllegalStateException("A required Extension parameter cannot have a default value");
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
    public DataType getType()
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
