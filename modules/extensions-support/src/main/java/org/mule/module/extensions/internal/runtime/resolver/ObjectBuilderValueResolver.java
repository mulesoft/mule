/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.extensions.internal.runtime.ObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ValueResolver} which wraps an {@link ObjectBuilder}
 * and calls {@link ObjectBuilder#build(MuleEvent)} on each
 * {@link #resolve(MuleEvent)}.
 * <p/>
 * It implements {@link Lifecycle} and propagates all lifecycle events to
 * the underlying {@code builder}
 *
 * @param <T>
 * @since 3.7.0
 */
public class ObjectBuilderValueResolver<T> implements ValueResolver<T>, Lifecycle, MuleContextAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectBuilderValueResolver.class);

    private final ObjectBuilder<T> builder;
    private MuleContext muleContext;

    public ObjectBuilderValueResolver(ObjectBuilder<T> builder)
    {
        checkArgument(builder != null, "builder cannot be null");
        this.builder = builder;
    }

    /**
     * Delegates to {@code builder}
     *
     * @param event a {@link MuleEvent}
     * @return the {@code builder}'s output object
     * @throws MuleException
     */
    @Override
    public T resolve(MuleEvent event) throws MuleException
    {
        return builder.build(event);
    }

    /**
     * @return {@code true} if {@code builder} is dynamic
     */
    @Override
    public boolean isDynamic()
    {
        return builder.isDynamic();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (builder instanceof MuleContextAware)
        {
            ((MuleContextAware) builder).setMuleContext(muleContext);
        }

        if (builder instanceof Initialisable)
        {
            ((Initialisable) builder).initialise();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (builder instanceof Startable)
        {
            ((Startable) builder).start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (builder instanceof Stoppable)
        {
            ((Stoppable) builder).stop();
        }
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(builder, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }
}
