/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

/**
 * An implementation of {@link ValueSetter} for assigning
 * the value of a single, non grouped {@link Parameter}
 * <p/>
 * For performance reasons, it caches the setter to be used
 *
 * @since 3.7.0
 */
public final class SingleValueSetter implements ValueSetter
{

    /**
     * The {@link Parameter} which this instance sets
     */
    private final Parameter parameter;

    /**
     * The {@link Field} in which the value is to be assigned
     */
    private final Field field;

    public SingleValueSetter(Parameter parameter, Field field)
    {
        this.parameter = parameter;
        this.field = field;
        field.setAccessible(true);
    }

    /**
     * Invokes {@link #field} over {@code target}, obtaining the value
     * from {@code result}
     *
     * @param target the object on which the value is being set
     * @param resolverSetResult a {@link ResolverSetResult} containing the value that corresponds to {@code parameter}
     */
    @Override
    public void set(Object target, ResolverSetResult resolverSetResult)
    {
        ReflectionUtils.setField(field, target, resolverSetResult.get(parameter));
    }
}
