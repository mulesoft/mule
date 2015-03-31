/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static java.lang.String.format;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A wrapper class to hold an instance which is a realisation of a
 * {@link Configuration} model and state associated to it, such as its
 * {@link OperationExecutor}s, registration name, etc.
 * <p/>
 * This class is also useful to use a {@link #configurationInstance} in hash-based
 * data structures without directly depending on its {@link #equals(Object)} or
 * {@link #hashCode()} implementations which we cannot control. Thus, this
 * class redefines such methods so that two instances are consider to be equal
 * if they refer to the same {@link #configurationInstance} and so that this
 * instance's hash is the {@link System#identityHashCode(Object)} of the
 * {@link #configurationInstance}
 *
 * @param <C> the type of the configuration instance
 * @since 3.7.0
 */
public final class ConfigurationInstanceWrapper<C>
{

    private final String name;
    private final C configurationInstance;
    private final ConcurrentMap<Operation, OperationExecutor> executors = new ConcurrentHashMap<>();

    public ConfigurationInstanceWrapper(String name, C configurationInstance)
    {
        checkArgument(!StringUtils.isEmpty(name), "name cannot be empty");
        checkArgument(configurationInstance != null, "configurationInstance cannot be null");

        this.name = name;
        this.configurationInstance = configurationInstance;
    }

    /**
     * @param operation the {@link Operation} you wan to execute
     * @return a {@link OperationExecutor} or {@code null} if none registered yet
     */
    OperationExecutor getOperationExecutor(Operation operation)
    {
        return executors.get(operation);
    }

    /**
     * @return the {@link #configurationInstance}
     */
    C getConfigurationInstance()
    {
        return configurationInstance;
    }

    /**
     * Registers a {@link OperationExecutor} for the given {@code operation}
     *
     * @param operation the {@link Operation}    you want to execute
     * @param executor  a {@link OperationExecutor}
     */
    void registerOperationExecutor(Operation operation, OperationExecutor executor)
    {
        if (executors.putIfAbsent(operation, executor) != null)
        {
            throw new IllegalStateException(format("An operation executor was already registered for operation %s on configuration instance %s", operation.getName(), getName()));
        }
    }

    /**
     * @return the instance registration's name
     */
    String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ConfigurationInstanceWrapper)
        {
            return configurationInstance == ((ConfigurationInstanceWrapper) obj).configurationInstance;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return System.identityHashCode(configurationInstance);
    }
}
