/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.extension.introspection.Described;
import org.mule.api.extension.introspection.EnrichableModel;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Base class for immutable implementations of the introspection model
 *
 * @since 4.0
 */
public abstract class AbstractImmutableModel implements Described, EnrichableModel
{

    private final String name;
    private final String description;
    private final Map<String, Object> modelProperties;

    /**
     * Creates a new instance
     *
     * @param name            the model's name
     * @param description     the model's description
     * @param modelProperties A {@link Map} of custom properties which extend this model
     * @throws IllegalArgumentException if {@code name} is blank
     */
    protected AbstractImmutableModel(String name, String description, Map<String, Object> modelProperties)
    {
        checkArgument(!StringUtils.isBlank(name), "Name attribute cannot be null or blank");

        this.name = name;
        this.description = description != null ? description : EMPTY;
        this.modelProperties = modelProperties != null ? ImmutableMap.copyOf(modelProperties) : ImmutableMap.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getDescription()
    {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getModelProperty(String key)
    {
        checkArgument(!StringUtils.isBlank(key), "A model property cannot have a blank key");
        return (T) modelProperties.get(key);
    }

    /**
     * Defines object equality based on the given object
     * being an object of this class and in the equality
     * of the {@link #getName()} attributes
     *
     * @param obj an object
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object obj)
    {
        if (getClass().isInstance(obj))
        {
            return name.equals(((Described) obj).getName());
        }

        return false;
    }

    /**
     * Calculates hashcode based on {@link #getName()}
     *
     * @return a hash code
     */
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
