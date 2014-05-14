/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.util;

import static org.mule.repackaged.internal.org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.api.MuleException;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Method;

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
     * The setter to be used when assigning the value
     */
    private final Method setter;

    public SingleValueSetter(Parameter parameter, Method setter)
    {
        this.parameter = parameter;
        this.setter = setter;
    }

    /**
     * Invokes {@link #setter} over {@code target}, obtaining the value
     * from {@code result}
     *
     * @param target the object on which the value is being set
     * @param result a {@link ResolverSetResult} containing the value that corresponds to {@code parameter}
     * @throws MuleException
     */
    @Override
    public void set(Object target, ResolverSetResult result) throws MuleException
    {
        invokeMethod(setter, target, result.get(parameter));
    }
}
