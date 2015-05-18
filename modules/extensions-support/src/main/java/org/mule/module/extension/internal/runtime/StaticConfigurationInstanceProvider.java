/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;

public final class StaticConfigurationInstanceProvider<T> implements ConfigurationInstanceProvider<T>
{

    private final String name;
    private final Configuration configuration;
    private final T configurationInstance;
    private ProviderDelegate<T> providerDelegate;

    public StaticConfigurationInstanceProvider(String name, Configuration configuration, T configurationInstance)
    {
        this.name = name;
        this.configuration = configuration;
        this.configurationInstance = configurationInstance;
        providerDelegate = new FirstTimeProviderDelegate<>();
    }

    @Override
    public T get(OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback)
    {
        try
        {
            return providerDelegate.provide(operationContext, registrationCallback);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

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

    private interface ProviderDelegate<T>
    {

        T provide(OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback) throws MuleException;
    }

    private class FirstTimeProviderDelegate<T> implements ProviderDelegate<T>
    {

        @Override
        public synchronized T provide(OperationContext operationContext,
                                      ConfigurationInstanceRegistrationCallback registrationCallback) throws MuleException
        {
            if (providerDelegate == this)
            {
                registrationCallback.registerNewConfigurationInstance(StaticConfigurationInstanceProvider.this, configurationInstance);
                providerDelegate = new FixedProviderDelegate<>(configurationInstance);
            }

            return (T) configurationInstance;
        }
    }

    private class FixedProviderDelegate<T> implements ProviderDelegate<T>
    {

        private T configurationInstance;

        private FixedProviderDelegate(T configurationInstance)
        {
            this.configurationInstance = configurationInstance;
        }

        @Override
        public T provide(OperationContext operationContext, ConfigurationInstanceRegistrationCallback registrationCallback) throws MuleException
        {
            return configurationInstance;
        }
    }
}
