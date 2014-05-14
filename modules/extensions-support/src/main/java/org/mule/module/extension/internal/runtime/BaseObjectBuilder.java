/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.module.extension.internal.util.MuleExtensionUtils;
import org.mule.repackaged.internal.org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for implementations of {@link ObjectBuilder}. It implements
 * all of the contract's behavior, except for how to actually
 * create the instance to be returned. Implementors must provide
 * that piece through the abstract {@link #instantiateObject()} method
 *
 * @since 3.7.0
 */
abstract class BaseObjectBuilder<T> implements ObjectBuilder<T>
{

    private final Map<Method, ValueResolver<Object>> resolvers = new HashMap<>();
    private final Map<Method, Object> values = new HashMap<>();

    /**
     * Returns the instance to be returned before the properties have
     * been applied to it
     */
    protected abstract T instantiateObject();

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectBuilder<T> addPropertyResolver(Method method, ValueResolver<? extends Object> resolver)
    {
        checkArgument(method != null, "method cannot be null");
        checkArgument(resolver != null, "resolver cannot be null");

        resolvers.put(method, (ValueResolver<Object>) resolver);
        return this;
    }

    @Override
    public ObjectBuilder<T> addPropertyValue(Method method, Object value)
    {
        checkArgument(method != null, "method cannot be null");

        values.put(method, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic()
    {
        return MuleExtensionUtils.hasAnyDynamic(resolvers.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T build(MuleEvent event) throws MuleException
    {
        T object = instantiateObject();

        for (Map.Entry<Method, ValueResolver<Object>> entry : resolvers.entrySet())
        {
            ReflectionUtils.invokeMethod(entry.getKey(), object, entry.getValue().resolve(event));
        }

        for (Map.Entry<Method, Object> entry : values.entrySet())
        {
            ReflectionUtils.invokeMethod(entry.getKey(), object, entry.getValue());
        }

        return object;
    }
}
