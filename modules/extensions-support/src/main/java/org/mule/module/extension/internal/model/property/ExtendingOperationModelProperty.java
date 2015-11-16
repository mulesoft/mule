/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.annotation.api.Extensible;
import org.mule.extension.annotation.api.Extension;

/**
 * A custom model property which marks that an operation is augmenting
 * the functionality of an {@link Extension} which is defined in a type
 * annotated with {@link Extensible}.
 * <p/>
 * The runtime consequences of this property depend on the runtime.
 * This class constructor throws {@link IllegalArgumentException} if
 * {@link #type} is not annotated with {@link Extensible}
 *
 * @since 4.0
 */
public final class ExtendingOperationModelProperty<T>
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = ExtendingOperationModelProperty.class.getName();

    private final Class<T> type;

    /**
     * Creates a new instance pointing to a {@code type} annotated
     * with {@link IllegalArgumentException}
     *
     * @param type the type that is being implemented
     * @throws IllegalArgumentException if {@code type} is not annotated with {@link Extensible}
     */
    public ExtendingOperationModelProperty(Class<T> type)
    {
        checkArgument(type != null, "cannot implement a null type");
        checkArgument(type.getAnnotation(Extensible.class) != null, type.getName() + " is not annotated with @Extensible");
        this.type = type;
    }

    /**
     * @return {@code type}
     */
    public Class<T> getType()
    {
        return type;
    }
}
