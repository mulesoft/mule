/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;

/**
 * {@link ConfigurationInstanceProvider} which provides always the same {@link #configurationInstance}.
 *
 * @param <T> the generic type of the instances provided
 * @since 3.7.0
 */
public final class StaticConfigurationInstanceProvider<T> implements ConfigurationInstanceProvider<T>
{

    private final T configurationInstance;

    public StaticConfigurationInstanceProvider(T configurationInstance)
    {
        this.configurationInstance = configurationInstance;
    }

    /**
     * Returns {@link #configurationInstance}.
     * <p/>
     * The first time this method is invoked, the instance
     * is registered on the {@code registrationCallback}
     *
     * @param operationContext     the {@link OperationContext context} of the {@link Operation} being executed
     * @return {@link #configurationInstance}
     */
    @Override
    public T get(OperationContext operationContext)
    {
        return configurationInstance;
    }
}
