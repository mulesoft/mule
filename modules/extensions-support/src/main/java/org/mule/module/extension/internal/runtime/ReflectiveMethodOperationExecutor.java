/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.toMap;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser;
import org.mule.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.ByParameterNameArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.EventArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.MessageArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import com.google.common.util.concurrent.Futures;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Implementation of {@link OperationExecutor} which relies on a
 * {@link #executorDelegate} and a reference to one of its methods.
 *
 * @since 3.7.0
 */
final class ReflectiveMethodOperationExecutor<D> implements DelegatingOperationExecutor<D>
{

    private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

    private final Method operationMethod;
    private final D executorDelegate;
    private final ReturnDelegate returnDelegate;
    private ArgumentResolverDelegate argumentResolverDelegate;

    ReflectiveMethodOperationExecutor(Method operationMethod, D executorDelegate, ReturnDelegate returnDelegate)
    {
        this.operationMethod = operationMethod;
        this.executorDelegate = executorDelegate;
        this.returnDelegate = returnDelegate;
        argumentResolverDelegate = isEmpty(operationMethod.getParameterTypes()) ? NO_ARGS_DELEGATE : new MethodArgumentResolverDelegate(operationMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Object> execute(OperationContext operationContext) throws Exception
    {
        Object result = invokeMethod(operationMethod, executorDelegate, getParameterValues(operationContext));
        return Futures.immediateFuture(returnDelegate.asReturnValue(result, operationContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public D getExecutorDelegate()
    {
        return executorDelegate;
    }

    private Object[] getParameterValues(OperationContext operationContext)
    {
        return argumentResolverDelegate.resolve(operationContext);
    }

    private interface ArgumentResolverDelegate
    {

        Object[] resolve(OperationContext operationContext);
    }

    private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate
    {

        private static Object[] EMPTY = new Object[] {};

        @Override
        public Object[] resolve(OperationContext operationContext)
        {
            return EMPTY;
        }
    }

    private class MethodArgumentResolverDelegate implements ArgumentResolverDelegate
    {

        private final Method method;
        private ArgumentResolver<? extends Object>[] argumentResolvers;

        public MethodArgumentResolverDelegate(Method method)
        {
            this.method = method;
            initArgumentResolvers();
        }

        private void initArgumentResolvers()
        {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (isEmpty(parameterTypes))
            {
                argumentResolvers = new ArgumentResolver[] {};
            }

            argumentResolvers = new ArgumentResolver[parameterTypes.length];
            Annotation[][] parameterAnnotations = operationMethod.getParameterAnnotations();
            final String[] paramNames = MuleExtensionAnnotationParser.getParamNames(operationMethod);

            for (int i = 0; i < parameterTypes.length; i++)
            {
                final Class<?> parameterType = parameterTypes[i];
                Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);
                if (MuleEvent.class.isAssignableFrom(parameterType))
                {
                    argumentResolvers[i] = new EventArgumentResolver();
                }
                else if (MuleMessage.class.isAssignableFrom(parameterType))
                {
                    argumentResolvers[i] = new MessageArgumentResolver();
                }
                else if (annotations.get(ParameterGroup.class) != null)
                {
                    argumentResolvers[i] = new ParameterGroupArgumentResolver(parameterType);
                }
                else
                {
                    argumentResolvers[i] = new ByParameterNameArgumentResolver<>(paramNames[i]);
                }
            }
        }

        @Override
        public Object[] resolve(OperationContext operationContext)
        {
            Object[] parameterValues = new Object[argumentResolvers.length];
            int i = 0;
            for (ArgumentResolver<?> argumentResolver : argumentResolvers)
            {
                parameterValues[i++] = argumentResolver.resolve(operationContext);
            }

            return parameterValues;
        }
    }

}
