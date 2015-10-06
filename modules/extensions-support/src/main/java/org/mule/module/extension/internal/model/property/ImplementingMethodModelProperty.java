/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.api.introspection.OperationModel;

import java.lang.reflect.Method;

/**
 * An immutable model property which indicates that the owning {@link OperationModel}
 * was derived from a given {@link #method}
 *
 * @since 4.0
 */
public final class ImplementingMethodModelProperty
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = ImplementingMethodModelProperty.class.getName();

    private final Method method;

    /**
     * Creates a new instance referencing the given {@code method}
     *
     * @param method a {@link Method} which defines the owning {@link OperationModel}
     * @throws IllegalArgumentException if {@code method} is {@code null}
     */
    public ImplementingMethodModelProperty(Method method)
    {
        checkArgument(method != null, "method cannot be null");
        this.method = method;
    }

    /**
     * @return a {@link Method} which defines the owning {@link OperationModel}
     */
    public Method getMethod()
    {
        return method;
    }
}
