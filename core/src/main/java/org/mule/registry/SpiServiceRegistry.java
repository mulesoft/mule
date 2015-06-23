/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.registry.ServiceRegistry;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * Implementation of {@link ServiceRegistry}
 * that uses standard {@link java.util.ServiceLoader} to get
 * the providers
 *
 * @since 3.7.0
 */
public class SpiServiceRegistry implements ServiceRegistry
{

    /**
     * If {@code classLoader} is {@code null}, then the current {@link Thread#getContextClassLoader()}
     * will be used
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> lookupProviders(Class<T> providerClass, ClassLoader classLoader)
    {
        if (classLoader == null)
        {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        return ImmutableList.copyOf(ServiceLoader.load(providerClass, classLoader).iterator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> lookupProviders(Class<T> providerClass)
    {
        return ImmutableList.copyOf(ServiceLoader.load(providerClass).iterator());
    }
}
