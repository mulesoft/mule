/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser.toMap;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.introspection.MuleExtensionAnnotationParser;
import org.mule.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.ByParameterNameArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.EventArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.MessageArgumentResolver;
import org.mule.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Resolves the values of an {@link Operation};s {@link Parameter}s by
 * matching them to the arguments in a {@link Method}
 *
 * @since 3.7.0
 */
final class MethodArgumentResolverDelegate implements ArgumentResolverDelegate
{

    private ReflectiveMethodOperationExecutor reflectiveMethodOperationExecutor;
    private final Method method;
    private ArgumentResolver<? extends Object>[] argumentResolvers;

    public MethodArgumentResolverDelegate(ReflectiveMethodOperationExecutor reflectiveMethodOperationExecutor, Method method)
    {
        this.reflectiveMethodOperationExecutor = reflectiveMethodOperationExecutor;
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

        Method operationMethod = reflectiveMethodOperationExecutor.getOperationMethod();
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
