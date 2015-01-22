/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
import org.mule.module.extensions.internal.util.MuleExtensionUtils;
import org.mule.repackaged.internal.org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementations of {@link ObjectBuilder}. It implements
 * all of the contract's behavior, except for how to actually
 * create the instance to be returned. Implementors must provide
 * that piece through the abstract {@link #instantiateObject()} method
 *
 * @since 3.7.0
 */
abstract class BaseObjectBuilder<T> implements ObjectBuilder<T>, Lifecycle, MuleContextAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultObjectBuilder.class);

    private final Map<Method, ValueResolver<Object>> resolvers = new HashMap<>();
    private final Map<Method, Object> values = new HashMap<>();
    private MuleContext muleContext;

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

    /**
     * For each registered {@link ValueResolver}, it propagates
     * the {link #muleContext} if it implements the {@link MuleContextAware}
     * interface and invokes {@link Initialisable#initialise()} if that
     * interface is also implemented by the resolver
     */
    @Override
    public void initialise() throws InitialisationException
    {
        for (ValueResolver<?> resolver : resolvers.values())
        {
            if (resolver instanceof MuleContextAware)
            {
                ((MuleContextAware) resolver).setMuleContext(muleContext);
            }
        }

        LifecycleUtils.initialiseIfNeeded(resolvers.values());
    }

    /**
     * For each registered {@link ValueResolver} it invokes
     * {@link Startable#start()} if the resolver implements that interface
     */
    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(resolvers.values());
    }

    /**
     * For each registered {@link ValueResolver} the
     * {@link Stoppable#stop()} method is invoked
     * if the resolver implements such interface
     */
    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(resolvers.values());
    }

    /**
     * For each registered {@link ValueResolver} the
     * {@link Disposable#dispose()} method is invoked
     * if the resolver implements such interface
     */
    @Override
    public void dispose()
    {
        LifecycleUtils.disposeAllIfNeeded(resolvers.values(), LOGGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }
}
