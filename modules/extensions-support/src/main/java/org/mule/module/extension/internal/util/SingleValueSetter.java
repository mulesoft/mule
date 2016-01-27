/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import org.mule.extension.api.introspection.ParameterModel;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Field;

/**
 * An implementation of {@link ValueSetter} for assigning
 * the value of a single, non grouped {@link ParameterModel}
 * <p/>
 * For performance reasons, it caches the setter to be used
 *
 * @since 3.7.0
 */
public final class SingleValueSetter implements ValueSetter
{

    /**
     * The {@link ParameterModel} which this instance sets
     */
    private final ParameterModel parameterModel;

    private final FieldSetter<Object, Object> fieldSetter;

    public SingleValueSetter(ParameterModel parameterModel, Field field)
    {
        this.parameterModel = parameterModel;
        this.fieldSetter = new FieldSetter<>(field);
    }

    /**
     * Sets the {@code resolverSetResult} value for the {@link #parameterModel}
     * into the {@link Field} supplied in the constructor
     *
     * @param target the object on which the value is being set
     * @param resolverSetResult a {@link ResolverSetResult} containing the value that corresponds to {@code parameter}
     */
    @Override
    public void set(Object target, ResolverSetResult resolverSetResult)
    {
        Object value = resolverSetResult.get(parameterModel);
        if (value != null)
        {
            fieldSetter.set(target, value);
        }
    }
}
