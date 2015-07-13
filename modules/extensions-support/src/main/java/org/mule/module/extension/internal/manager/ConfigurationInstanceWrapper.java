/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.util.StringUtils;

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

    public ConfigurationInstanceWrapper(String name, C configurationInstance)
    {
        checkArgument(!StringUtils.isEmpty(name), "name cannot be empty");
        checkArgument(configurationInstance != null, "configurationInstance cannot be null");

        this.name = name;
        this.configurationInstance = configurationInstance;
    }

    /**
     * @return the {@link #configurationInstance}
     */
    C getConfigurationInstance()
    {
        return configurationInstance;
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
