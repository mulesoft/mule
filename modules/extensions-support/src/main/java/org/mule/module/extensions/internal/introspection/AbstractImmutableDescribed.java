/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Described;

import org.apache.commons.lang.StringUtils;

/**
 * Abstract implementation to act as a convenience superclass for
 * implementations of {@link Described}
 *
 * @since 3.7.0
 */
abstract class AbstractImmutableDescribed implements Described
{

    private final String name;
    private final String description;

    protected AbstractImmutableDescribed(String name, String description)
    {
        checkArgument(!StringUtils.isBlank(name), "Name attribute cannot be null or blank");

        this.name = name;
        this.description = description != null ? description : EMPTY;
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
     * calculates hashcode based on {@link #getName()}
     *
     * @return a hash code
     */
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
