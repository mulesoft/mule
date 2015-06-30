/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.asOperationContextAdapter;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A {@link ConfigurationInstanceProvider} which continuously evaluates the same
 * {@link ResolverSet} and then uses the resulting {@link ResolverSetResult}
 * to build an instance of a given type.
 * <p/>
 * Although each invocation to {@link #get(OperationContext, ConfigurationInstanceRegistrationCallback)}
 * is guaranteed to end up in an invocation to {@link #resolverSet#resolve(MuleEvent)}, the resulting
 * {@link ResolverSetResult} might not end up generating a new instance. This is so because
 * {@link ResolverSetResult} instances are put in a cache to
 * guarantee that equivalent evaluations of the {@code resolverSet} return the same
 * instance.
 *
 * @since 3.7.0
 */
public final class DynamicConfigurationInstanceProvider<T> implements ConfigurationInstanceProvider<T>
{

    private final String name;
    private final Configuration configuration;
    private final ConfigurationObjectBuilder configurationObjectBuilder;
    private final ResolverSet resolverSet;

    private LoadingCache<CacheKey, T> cache;

    /**
     * Creates a new instance
     *
     * @param name                       the name of the config definition
     * @param configuration              the {@link Configuration} model
     * @param configurationObjectBuilder the {@link ConfigurationObjectBuilder} that will build the configuration instances
     * @param resolverSet                the {@link ResolverSet} that's going to be evaluated
     */
    public DynamicConfigurationInstanceProvider(String name,
                                                Configuration configuration,
                                                ConfigurationObjectBuilder configurationObjectBuilder,
                                                ResolverSet resolverSet)
    {
        this.name = name;
        this.configuration = configuration;
        this.configurationObjectBuilder = configurationObjectBuilder;
        this.resolverSet = resolverSet;
        buildCache();
    }

    private void buildCache()
    {
        cache = CacheBuilder.newBuilder().build(new CacheLoader<CacheKey, T>()
        {
            @Override
            public T load(CacheKey key) throws Exception
            {
                T configurationInstance = (T) configurationObjectBuilder.build(key.resolverSetResult);
                key.registrationCallback.registerNewConfigurationInstance(DynamicConfigurationInstanceProvider.this,
                                                                          configurationInstance);

                return configurationInstance;
            }
        });
    }

    /**
     * Evaluates {@link #resolverSet} using the given {@code event} and returns
     * an instance produced with the result. For equivalent {@link ResolverSetResult}s
     * it will return the same instance, for as long as the {@code expirationInterval} and
     * {@code expirationTimeUnit} were specified in the constructor
     *
     * @param operationContext     a {@link OperationContext}
     * @param registrationCallback a {@link ConfigurationInstanceRegistrationCallback} to be invoked when a new instance is produced
     * @return the resolved value
     */
    @Override
    public T get(OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback)
    {
        try
        {
            ResolverSetResult result = resolverSet.resolve(asOperationContextAdapter(operationContext).getEvent());
            return cache.getUnchecked(new CacheKey(result, registrationCallback));
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    @Override
    public String getName()
    {
        return name;
    }

    private static class CacheKey
    {

        private final ResolverSetResult resolverSetResult;
        private final ConfigurationInstanceRegistrationCallback registrationCallback;

        private CacheKey(ResolverSetResult resolverSetResult, ConfigurationInstanceRegistrationCallback registrationCallback)
        {
            this.resolverSetResult = resolverSetResult;
            this.registrationCallback = registrationCallback;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof CacheKey)
            {
                return resolverSetResult.equals(((CacheKey) obj).resolverSetResult);
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            return resolverSetResult.hashCode();
        }
    }
}
