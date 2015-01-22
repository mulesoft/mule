/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extensions.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.module.extensions.internal.util.IntrospectionUtils.getImplementedTypes;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extensions.introspection.OperationContext;
import org.mule.extensions.introspection.OperationImplementation;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.capability.metadata.ImplicitArgumentCapability;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extensions.internal.util.ValueSetter;
import org.mule.repackaged.internal.org.springframework.util.ReflectionUtils;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Futures;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation of {@link OperationImplementation} which relies on a
 * {@link Class} and a reference to one of its methods. On each execution,
 * that class instantiated and the method invoked.
 *
 * @since 3.7.0
 */
public final class TypeAwareOperationImplementation<T> implements OperationImplementation
{

    private static final ReturnDelegate VOID_RETURN_DELEGATE = new VoidReturnDelegate();
    private static final ReturnDelegate VALUE_RETURN_DELEGATE = new ValueReturnDelegate();

    private final Class<T> actingClass;
    private final Method operationMethod;
    private final ReturnDelegate returnDelegate;
    private final ConfigurationInjector<Object, T> configurationInjector;

    public TypeAwareOperationImplementation(Class<T> actingClass, Method operationMethod)
    {
        validateActingClass(actingClass);
        checkArgument(operationMethod != null, "operation method cannot be null");
        this.actingClass = actingClass;
        this.operationMethod = operationMethod;
        returnDelegate = isVoid() ? VOID_RETURN_DELEGATE : VALUE_RETURN_DELEGATE;
        configurationInjector = ConfigurationInjector.of(actingClass);
    }

    @Override
    public Future<Object> execute(OperationContext operationContext) throws Exception
    {
        Object result = ReflectionUtils.invokeMethod(operationMethod, newOperationInstance(operationContext), getParameterValues(operationContext));
        return Futures.immediateFuture(returnDelegate.asReturnValue(result, operationContext));
    }

    private Object[] getParameterValues(OperationContext operationContext)
    {
        Map<Parameter, Object> parameters = operationContext.getParametersValues();
        List<Object> values = new ArrayList<>(parameters.size());
        for (Map.Entry<Parameter, Object> parameter : parameters.entrySet())
        {
            if (!parameter.getKey().isCapableOf(ImplicitArgumentCapability.class))
            {
                values.add(parameter.getValue());
            }
        }

        return values.toArray();
    }

    private T newOperationInstance(OperationContext context) throws MuleException
    {
        T instance;

        try
        {
            instance = actingClass.newInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(String.format("Implementation type %s could not be instantiated", actingClass.getName())), e);
        }

        configurationInjector.injectConfiguration(context.getConfigurationInstance(), instance);
        setInstanceLevelParameterGroups(instance, context);

        return instance;
    }

    private void setInstanceLevelParameterGroups(Object instance, OperationContext context) throws MuleException
    {
        List<ValueSetter> groupSetters = ((DefaultOperationContext) context).getGroupValueSetters();
        if (!CollectionUtils.isEmpty(groupSetters))
        {
            ResolverSetResult resolverSetResult = ((DefaultOperationContext) context).getParameters();
            for (ValueSetter setter : groupSetters)
            {
                setter.set(instance, resolverSetResult);
            }
        }
    }

    private boolean isVoid()
    {
        Class<?> returnType = operationMethod.getReturnType();
        return returnType.equals(void.class) || returnType.equals(Void.class);
    }

    private void validateActingClass(Class<T> actingClass)
    {
        checkInstantiable(actingClass);
        Collection<Class<?>> lifecycleTypes = getImplementedTypes(actingClass, Initialisable.class, Startable.class, Stoppable.class, Disposable.class);

        if (!CollectionUtils.isEmpty(lifecycleTypes))
        {
            throw new IllegalArgumentException(String.format(
                    "Operation implementation objects cannot have lifecycle but type %s implements the following: [%s]",
                    actingClass.getName(), Joiner.on(',').join(lifecycleTypes)));
        }
    }

    private interface ReturnDelegate
    {

        Object asReturnValue(Object value, OperationContext operationContext);
    }

    private static class VoidReturnDelegate implements ReturnDelegate
    {

        @Override
        public Object asReturnValue(Object value, OperationContext operationContext)
        {
            return ((DefaultOperationContext) operationContext).getEvent();
        }
    }

    private static class ValueReturnDelegate implements ReturnDelegate
    {

        @Override
        public Object asReturnValue(Object value, OperationContext operationContext)
        {
            return value;
        }
    }
}
