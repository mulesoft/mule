/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.metadata;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.Capable;

/**
 * A metadata capability to link a {@link Capable} object
 * to a certain {@link #type} which restricts it somehow.
 *
 * @param <T> generic type of the restriction {@link #type}
 * @since 3.7.0
 */
public final class TypeRestrictionCapability<T>
{
    private final Class<T> type;

    public TypeRestrictionCapability(Class<T> type)
    {
        checkArgument(type != null, "cannot restrict to a null type");
        this.type = type;
    }

    public Class<T> getType()
    {
        return type;
    }
}
