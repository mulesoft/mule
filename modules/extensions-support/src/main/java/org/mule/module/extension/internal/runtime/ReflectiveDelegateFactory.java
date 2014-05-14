/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static java.lang.String.format;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.reflections.ReflectionUtils.getConstructors;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersAssignableTo;
import static org.reflections.ReflectionUtils.withParametersCount;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Operation;
import org.mule.module.extension.internal.manager.ConfigurationInstanceWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A factory to create delegate objects to be used by {@link DelegatingOperationExecutor} instances.
 * That delegate is to be created by receiving the delegates {@link Class} and a {@code configurationInstance}
 * that it will be related to.
 * <p/>
 * The provided {@link Class} is expected to have a public {@link Constructor} which takes a configuration instance
 * as its only argument, since per the rules of {@link ExtensionManager#getOperationExecutor(Operation, Object)}
 * <p/>
 * This factory will ensure that for a given {@code delegateType}|{@code configurationInstance} pair, the same
 * delegate will always be returned. This will allow for the delegate objects to safely store state
 * for a given configuration
 *
 * @since 3.7.0
 */
public final class ReflectiveDelegateFactory
{

    private ConcurrentMap<DelegateKey, Object> delegates = new ConcurrentHashMap<>();

    public <C, D> D getDelegate(Class<D> delegateType, C configurationInstance)
    {
        DelegateKey key = new DelegateKey(delegateType, configurationInstance);
        synchronized (configurationInstance)
        {
            D delegate = (D) delegates.get(key);
            if (delegate == null)
            {
                delegate = createDelegate(delegateType, configurationInstance);
                delegates.put(key, delegate);
            }

            return delegate;
        }
    }

    private <C, D> D createDelegate(Class<D> delegateType, C configurationInstance)
    {
        Set<Constructor> suitableConstructors = getConstructors(delegateType,
                                                                withModifier(Modifier.PUBLIC),
                                                                withParametersCount(1),
                                                                withParametersAssignableTo(configurationInstance.getClass()));
        if (suitableConstructors.isEmpty())
        {
            throw new IllegalArgumentException(format("Class %s was expected to have one public constructor with one argument of type %s but it was not found. Add such constructor in order" +
                                                      "to execute those operations with a configuration of that type", delegateType.getName(), configurationInstance.getClass().getName()));
        }

        D delegate;
        try
        {
            delegate = (D) suitableConstructors.iterator().next().newInstance(configurationInstance);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(format("Could not create instance of class %s using configuration of type %s",
                                                                      delegateType.getName(), configurationInstance.getClass().getName())),
                                           e);
        }

        return delegate;
    }

    private class DelegateKey
    {

        private final Class<?> delegateType;
        private final ConfigurationInstanceWrapper<Object> configurationInstance;
        private final int hashCode;

        private DelegateKey(Class<?> delegateType, Object configurationInstance)
        {
            this.delegateType = delegateType;
            this.configurationInstance = new ConfigurationInstanceWrapper<>(delegateType.getName(), configurationInstance);
            hashCode = 31 * delegateType.hashCode() * configurationInstance.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof DelegateKey)
            {
                DelegateKey otherKey = (DelegateKey) obj;
                return delegateType == otherKey.delegateType && configurationInstance.equals(otherKey.configurationInstance);
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }
    }
}
