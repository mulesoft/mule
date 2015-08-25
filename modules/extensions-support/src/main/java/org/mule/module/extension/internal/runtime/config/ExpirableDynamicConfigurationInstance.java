/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.extension.runtime.Expirable;
import org.mule.extension.runtime.ExpirationPolicy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decorates a {@link #configurationInstance} with extra
 * management information such as registration name, usage tracking, etc.
 * <p/>
 * This class is thread safe.
 *
 * @since 4.0
 */
final class ExpirableDynamicConfigurationInstance implements Expirable
{

    private final String registrationName;
    private final Object configurationInstance;

    private final AtomicInteger usageCount = new AtomicInteger(0);
    private long lastUsedMillis = now();

    /**
     * Creates a new instance
     *
     * @param registrationName      the name under which {@code configurationInstance} was registered
     * @param configurationInstance the actual configuration instance being decorated
     */
    public ExpirableDynamicConfigurationInstance(String registrationName, Object configurationInstance)
    {
        this.registrationName = registrationName;
        this.configurationInstance = configurationInstance;
    }

    /**
     * Determines if the held {@link #configurationInstance} should be expired per
     * the given {@code expirationPolicy}.
     * <p/>
     * The first thing this method checks is if there's any ongoing operation using the held
     * configuration instance. If that's not the case, then it compares the timestamp of when
     * the {@link #configurationInstance} was last used against the {@code expirationPolicy}
     *
     * @param expirationPolicy a {@link ExpirationPolicy}
     * @return {@code true} if the {@link #configurationInstance} should be expired. {@code false} otherwise
     */
    @Override
    public boolean isExpired(ExpirationPolicy expirationPolicy)
    {
        return !isInUse() && expirationPolicy.isExpired(lastUsedMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the held configuration instance
     */
    public Object getConfigurationInstance()
    {
        return configurationInstance;
    }

    /**
     * acknowledges and tracks the fact that there's an ongoing operation execution
     * using the {@link #configurationInstance}. This method is thread safe.
     *
     * @return the updated usage count.
     */
    public int accountUsage()
    {
        return usageCount.incrementAndGet();
    }

    /**
     * acknowledges and tracks the fact that an ongoing operation execution
     * using the {@link #configurationInstance} has finished. This method is thread safe.
     *
     * @return the updated usage count.
     */
    public int discountUsage()
    {
        lastUsedMillis = now();
        return usageCount.decrementAndGet();
    }

    /**
     * The name under which the {@link #configurationInstance} has been registered
     *
     * @return
     */
    public String getRegistrationName()
    {
        return registrationName;
    }

    /**
     * @return {@code true} if there's no ongoing operation using the {@link #configurationInstance}. {@code false} otherwise
     */
    private boolean isInUse()
    {
        return usageCount.get() > 0;
    }

    private long now()
    {
        return System.currentTimeMillis();
    }
}
