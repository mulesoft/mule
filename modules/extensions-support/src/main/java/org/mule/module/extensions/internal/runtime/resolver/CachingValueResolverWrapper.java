/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Stoppable;

/**
 * A wrapper for an instance of {@link ValueResolver} in which
 * the delegate is only invoked once and the obtained value is cached.
 * This is useful in cases in which its expensive for the {@link #delegate}
 * to resolve the value.
 * <p/>
 * Care should be taken when using this class to wrap resolvers which
 * {@link #isDynamic()} method return {@code true}. Since this wrapper
 * will cache the result of the first invocation, the resolver's dynamism
 * is in effect neutralized.
 * <p/>
 * This class is thread safe. Many threads can invoke the {@link #resolve(MuleEvent)}
 * method and the underlying {@link #delegate} is still guaranteed to be invoked only once
 * and the return value to be consistent with that of the thread which first got access to it.
 * <p/>
 * This class can also be used without performance considerations since it's optimized to only
 * perform thread contention until a value is cached. From then on, no locks will be used while
 * remaining thread safe
 *
 * @since 3.7.0
 */
public class CachingValueResolverWrapper<T> extends BaseValueResolverWrapper<T>
{

    private T value;
    private CachingDelegate cacheDelegate = new FirstTimeCachingDelegate();

    public CachingValueResolverWrapper(ValueResolver<T> delegate)
    {
        super(delegate);
    }

    /**
     * Upon first invocation, the value is resolved using the {@link #delegate}
     * and the return value is stored. From then on, the stored value is always
     * returned
     *
     * @param event a {@link MuleEvent}
     * @return the result of the first invocation
     * @throws Exception
     */
    @Override
    public T resolve(MuleEvent event) throws MuleException
    {
        return (T) cacheDelegate.get(event);
    }

    /**
     * returns {@code false} since the same value will be
     * returned in each invocation
     */
    @Override
    public boolean isDynamic()
    {
        return false;
    }

    /**
     * Invokes {@link Stoppable#stop()} on the stored value
     * and the {@link #delegate} if they implement that interface
     *
     * @throws MuleException
     */
    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(value);
        super.stop();
    }

    /**
     * Invokes {@link Disposable#dispose()} ()} on the stored value
     * and the {@link #delegate} if they implement that interface
     *
     * @throws MuleException
     */
    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(value, logger);
        super.dispose();
    }

    private interface CachingDelegate
    {

        Object get(MuleEvent event) throws MuleException;
    }

    private class FirstTimeCachingDelegate implements CachingDelegate
    {

        @Override
        public synchronized Object get(MuleEvent event) throws MuleException
        {
            if (value == null)
            {
                value = delegate.resolve(event);
                cacheDelegate = new FastCachingDelegate();
            }

            return value;
        }
    }

    private class FastCachingDelegate implements CachingDelegate
    {

        @Override
        public Object get(MuleEvent event) throws MuleException
        {
            return value;
        }
    }
}
