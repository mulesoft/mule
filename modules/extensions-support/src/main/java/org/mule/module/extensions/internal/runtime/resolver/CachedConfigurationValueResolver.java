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
import org.mule.module.extensions.internal.runtime.ConfigurationObjectBuilder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ValueResolver} which continuously evaluates the same
 * {@link ResolverSet} and then uses the resulting {@link ResolverSetResult}
 * to build an instance of a given type.
 * <p/>
 * Although each invocation to {@link #resolve(MuleEvent)} is guaranteed to end up
 * in an invocation to {@link #resolverSet#resolve(MuleEvent)}, the resulting
 * {@link ResolverSetResult} might not end up generating a new instance. This is so because
 * {@link ResolverSetResult} instances are put in a cache to
 * guarantee that equivalent evaluations of the {@code resolverSet} return the same
 * instance. That cache will automatically expire entries that are not used for
 * an interval configured using {@code expirationInterval}
 * and {@code expirationTimeUnit}.
 * <p/>
 * When instances are evicted from the cache, the {@link Stoppable#stop()} and
 * {@link Disposable#dispose()} methods are invoked on them if they implement the
 * corresponding interfaces
 *
 * @since 3.7.0
 */
public class CachedConfigurationValueResolver implements ValueResolver<Object>, Stoppable, Disposable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedConfigurationValueResolver.class);

    private final ConfigurationObjectBuilder configurationObjectBuilder;
    private final ResolverSet resolverSet;

    private final LoadingCache<ResolverSetResult, Object> cache;

    /**
     * Creates a new instance
     *
     * @param configurationObjectBuilder the introspection model of the objects this resolver produces
     * @param resolverSet                the {@link ResolverSet} that's going to be evaluated
     * @param expirationInterval         the interval for which {@link ResolverSetResult}s are to be cached
     * @param expirationTimeUnit         the {@link TimeUnit} corresponding to {@code expirationInterval}
     */
    public CachedConfigurationValueResolver(ConfigurationObjectBuilder configurationObjectBuilder, ResolverSet resolverSet, long expirationInterval, TimeUnit expirationTimeUnit)
    {
        this.configurationObjectBuilder = configurationObjectBuilder;
        this.resolverSet = resolverSet;
        cache = buildCache(expirationInterval, expirationTimeUnit);
    }

    private LoadingCache<ResolverSetResult, Object> buildCache(long expirationInterval, TimeUnit expirationTimeUnit)
    {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(expirationInterval, expirationTimeUnit)
                .removalListener(new EvictionListener())
                .build(new CacheLoader<ResolverSetResult, Object>()
                {
                    @Override
                    public Object load(ResolverSetResult key) throws Exception
                    {
                        return configurationObjectBuilder.build(key);
                    }
                });
    }

    /**
     * Evaluates {@link #resolverSet} using the given {@code event} and returns
     * an instance produced with the result. For equivalent {@link ResolverSetResult}s
     * it will return the same instance, for as long as the {@code expirationInterval} and
     * {@code expirationTimeUnit} were specified in the constructor
     *
     * @param event a {@link MuleEvent}
     * @return the resolved value
     */
    @Override
    public Object resolve(MuleEvent event) throws MuleException
    {
        ResolverSetResult result = resolverSet.resolve(event);
        return cache.getUnchecked(result);
    }

    /**
     * Whether or not {@link #resolverSet} is dynamic
     */
    @Override
    public boolean isDynamic()
    {
        return resolverSet.isDynamic();
    }

    /**
     * invokes {@link Stoppable#stop()} on all the cached values
     * that implement such interface
     *
     * @throws MuleException
     */
    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(cache.asMap().values());
    }

    /**
     * invokes {@link Disposable#dispose()} on all the cached values
     * that implement such interface
     *
     * @throws MuleException
     */
    @Override
    public void dispose()
    {
        LifecycleUtils.disposeAllIfNeeded(cache.asMap().values(), LOGGER);
    }

    /**
     * Forces the cache to remove elements eligible for eviction.
     * This method should not be manually invoked on regular basis.
     * It's just for management operations and testing. The cache will perform
     * automatic cleanup when necessary.
     * <p/>
     * Invoking this method does not guarantee that any elements will be evicted
     * from the cache
     */
    public void cleanUpCache()
    {
        cache.cleanUp();
    }

    private class EvictionListener implements RemovalListener<ResolverSetResult, Object>
    {

        @Override
        public void onRemoval(RemovalNotification<ResolverSetResult, Object> notification)
        {
            Object value = notification.getValue();
            if (value == null)
            {
                return;
            }

            if (value instanceof Stoppable)
            {
                try
                {
                    ((Stoppable) value).stop();
                }
                catch (MuleException e)
                {
                    LOGGER.error("Found exception trying to stop instance of " + value.getClass().getName(), e);
                }
            }

            LifecycleUtils.disposeIfNeeded(value, LOGGER);
        }
    }
}
