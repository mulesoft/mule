/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import org.mule.api.extension.introspection.EnrichableModel;

/**
 * An immutable model property which indicates that the owning {@link EnrichableModel}
 * was derived from a given {@link #type}
 *
 * @since 4.0
 */
public final class ImplementingTypeModelProperty
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = ImplementingTypeModelProperty.class.getName();

    private final Class<?> type;

    /**
     * Creates a new instance referencing the given {@code type}
     *
     * @param type a {@link Class} which defines the owning {@link EnrichableModel}
     */
    public ImplementingTypeModelProperty(Class<?> type)
    {
        this.type = type;
    }

    /**
     * @return a {@link Class} which defines the owning {@link EnrichableModel}
     */
    public Class<?> getType()
    {
        return type;
    }
}
