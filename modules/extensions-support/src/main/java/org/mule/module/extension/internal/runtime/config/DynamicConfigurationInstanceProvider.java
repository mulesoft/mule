/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.asOperationContextAdapter;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.ExpirationPolicy;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.ExpirableContainer;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ConfigurationInstanceProvider} which continuously evaluates the same
 * {@link ResolverSet} and then uses the resulting {@link ResolverSetResult}
 * to build an instance of a given type.
 * <p/>
 * Although each invocation to {@link #get(OperationContext)} is guaranteed to end up in an invocation
 * to {@link #resolverSet#resolve(MuleEvent)}, the resulting {@link ResolverSetResult} might not end up
 * generating a new instance. This is so because {@link ResolverSetResult} instances are put in a cache to
 * guarantee that equivalent evaluations of the {@code resolverSet} return the same instance.
 *
 * @since 4.0.0
 */
public final class DynamicConfigurationInstanceProvider<T> implements ConfigurationInstanceProvider<T>, ExpirableContainer<Object>
{

    private final String name;
    private final Extension extension;
    private final ConfigurationInstanceRegistrationCallback registrationCallback;
    private final ConfigurationObjectBuilder configurationObjectBuilder;
    private final ResolverSet resolverSet;
    private final ExpirationPolicy expirationPolicy;

    private final Map<ResolverSetResult, ExpirableDynamicConfigurationInstance> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Lock cacheReadLock = cacheLock.readLock();
    private final Lock cacheWriteLock = cacheLock.writeLock();

    /**
     * Creates a new instance
     *
     * @param configurationObjectBuilder the {@link ConfigurationObjectBuilder} that will build the configuration instances
     * @param resolverSet                the {@link ResolverSet} that's going to be evaluated
     */
    public DynamicConfigurationInstanceProvider(String name,
                                                Extension extension,
                                                ConfigurationInstanceRegistrationCallback registrationCallback,
                                                ConfigurationObjectBuilder configurationObjectBuilder,
                                                ResolverSet resolverSet,
                                                ExpirationPolicy expirationPolicy)
    {
        this.name = name;
        this.extension = extension;
        this.registrationCallback = registrationCallback;
        this.configurationObjectBuilder = configurationObjectBuilder;
        this.resolverSet = resolverSet;
        this.expirationPolicy = expirationPolicy;
    }

    /**
     * Evaluates {@link #resolverSet} using the given {@code event} and returns
     * an instance produced with the result. For equivalent {@link ResolverSetResult}s
     * it will return the same instance.
     *
     * @param operationContext a {@link OperationContext}
     * @return the resolved value
     */
    @Override
    public T get(OperationContext operationContext)
    {
        try
        {
            ResolverSetResult result = resolverSet.resolve(asOperationContextAdapter(operationContext).getEvent());
            ExpirableDynamicConfigurationInstance configurationInstance = getConfigurationInstance(result);

            operationContext.onOperationSuccessful(signal -> discountUsage(configurationInstance));
            operationContext.onOperationFailed(signal -> discountUsage(configurationInstance));

            return (T) configurationInstance.getConfigurationInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private ExpirableDynamicConfigurationInstance getConfigurationInstance(ResolverSetResult resolverSetResult) throws Exception
    {
        ExpirableDynamicConfigurationInstance configurationInstance;
        cacheReadLock.lock();
        try
        {
            configurationInstance = cache.get(resolverSetResult);
            if (configurationInstance != null)
            {
                //important to account between the boundaries of the lock to prevent race condition
                configurationInstance.accountUsage();
                return configurationInstance;
            }
        }
        finally
        {
            cacheReadLock.unlock();
        }

        cacheWriteLock.lock();
        try
        {
            // re-check in case some other thread beat us to it...
            configurationInstance = cache.get(resolverSetResult);
            if (configurationInstance == null)
            {
                configurationInstance = createConfigurationInstance(resolverSetResult);
                cache.put(resolverSetResult, configurationInstance);
            }

            // accounting here for the same reasons as above
            configurationInstance.accountUsage();
            return configurationInstance;
        }
        finally
        {
            cacheWriteLock.unlock();
        }
    }

    private ExpirableDynamicConfigurationInstance createConfigurationInstance(ResolverSetResult result) throws MuleException
    {
        Object configurationInstance = configurationObjectBuilder.build(result);
        String registrationName = registrationCallback.registerConfigurationInstance(extension, name, configurationInstance);
        return new ExpirableDynamicConfigurationInstance(registrationName, configurationInstance);
    }

    @Override
    public Map<String, Object> getExpired()
    {
        ImmutableMap.Builder<String, Object> expiredConfigs = ImmutableMap.builder();
        cacheWriteLock.lock();
        try
        {
            cache.entrySet()
                    .stream()
                    .filter(config -> config.getValue().isExpired(expirationPolicy))
                    .forEach(config -> {
                        cache.remove(config.getKey());
                        ExpirableDynamicConfigurationInstance configurationInstance = config.getValue();
                        expiredConfigs.put(configurationInstance.getRegistrationName(), configurationInstance.getConfigurationInstance());
                    });
        }
        finally
        {
            cacheWriteLock.unlock();
        }

        return expiredConfigs.build();
    }

    private void discountUsage(ExpirableDynamicConfigurationInstance configurationInstanceHolder)
    {
        configurationInstanceHolder.discountUsage();
    }
}
