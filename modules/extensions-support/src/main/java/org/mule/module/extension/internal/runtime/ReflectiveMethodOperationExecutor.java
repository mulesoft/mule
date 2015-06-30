/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;

import com.google.common.util.concurrent.Futures;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * Implementation of {@link OperationExecutor} which relies on a
 * {@link #executorDelegate} and a reference to one of its methods.
 *
 * @param <D> the generic type of the {@link #executorDelegate} instance
 * @since 3.7.0
 */
public final class ReflectiveMethodOperationExecutor<D> implements DelegatingOperationExecutor<D>
{

    private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

    private final Method operationMethod;
    private final D executorDelegate;
    private final ReturnDelegate returnDelegate;
    private final ArgumentResolverDelegate argumentResolverDelegate;

    ReflectiveMethodOperationExecutor(Method operationMethod, D executorDelegate, ReturnDelegate returnDelegate)
    {
        this.operationMethod = operationMethod;
        this.executorDelegate = executorDelegate;
        this.returnDelegate = returnDelegate;
        argumentResolverDelegate = isEmpty(operationMethod.getParameterTypes()) ? NO_ARGS_DELEGATE : new MethodArgumentResolverDelegate(this, operationMethod);
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

    private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate
    {

        private static Object[] EMPTY = new Object[] {};

        @Override
        public Object[] resolve(OperationContext operationContext)
        {
            return EMPTY;
        }
    }

    Method getOperationMethod()
    {
        return operationMethod;
    }
}
