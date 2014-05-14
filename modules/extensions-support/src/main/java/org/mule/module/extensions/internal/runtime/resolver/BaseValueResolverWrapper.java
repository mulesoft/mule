/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a {@link ValueResolver} which is a wrapper of another one.
 * This wrapper implements {@link MuleContextAware} and {@link Lifecycle}
 * and propagates those events to the {@link #delegate} if it implements any of the
 * mentioned interfaces.
 *
 * @since 3.7.0
 */
abstract class BaseValueResolverWrapper<T> implements ValueResolver<T>, Lifecycle, MuleContextAware
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ValueResolver<T> delegate;
    protected MuleContext muleContext;

    BaseValueResolverWrapper(ValueResolver<T> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Resolves delegating into {@link #delegate#isDynamic()}
     *
     * @return whether the {@link #delegate} is dynamic or not
     */
    @Override
    public boolean isDynamic()
    {
        return delegate.isDynamic();
    }

    /**
     * Sets the {@link MuleContext} on the {@link #delegate} if it implements
     * {@link MuleContextAware}. Then, if it invokes {@link Initialisable#initialise()}
     * if the delegate implements such interface
     *
     * @throws InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        if (delegate instanceof MuleContextAware)
        {
            ((MuleContextAware) delegate).setMuleContext(muleContext);
        }

        if (delegate instanceof Initialisable)
        {
            ((Initialisable) delegate).initialise();
        }
    }

    /**
     * Invokes {@link Startable#start()}  on the {@link #delegate}
     * if it implements that interface
     *
     * @throws MuleException
     */
    @Override
    public void start() throws MuleException
    {
        if (delegate instanceof Startable)
        {
            ((Startable) delegate).start();
        }
    }

    /**
     * Invokes {@link Stoppable#stop()}  on the {@link #delegate}
     * if it implements that interface
     *
     * @throws MuleException
     */
    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(delegate);
    }

    /**
     * Invokes {@link Disposable#dispose()}  on the {@link #delegate}
     * if it implements that interface
     *
     * @throws MuleException
     */
    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(delegate, logger);
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
