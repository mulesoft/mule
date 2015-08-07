/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OperationExecutor} which relies on a
 * {@link #executorDelegate} and a reference to one of its {@link Method}s.
 * When {@link #execute(OperationContext)} is invoked, the {@link #operationMethod}
 * is invoked over the {@link #executorDelegate}.
 * <p/>
 * All the {@link Lifecycle} events that {@code this} instance receives are propagated
 * to the {@link #executorDelegate}
 *
 * @since 3.7.0
 */
public final class ReflectiveMethodOperationExecutor implements OperationExecutor, MuleContextAware, Lifecycle
{

    private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate
    {

        private static final Object[] EMPTY = new Object[] {};

        @Override
        public Object[] resolve(OperationContext operationContext)
        {
            return EMPTY;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectiveMethodOperationExecutor.class);
    private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

    private final Method operationMethod;
    private final Object executorDelegate;
    private final ReturnDelegate returnDelegate;
    private final ArgumentResolverDelegate argumentResolverDelegate;

    private MuleContext muleContext;

    ReflectiveMethodOperationExecutor(Method operationMethod, Object executorDelegate, ReturnDelegate returnDelegate)
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
    public Object execute(OperationContext operationContext) throws Exception
    {
        Object result = invokeMethod(operationMethod, executorDelegate, getParameterValues(operationContext));
        return returnDelegate.asReturnValue(result, operationContext);
    }

    private Object[] getParameterValues(OperationContext operationContext)
    {
        return argumentResolverDelegate.resolve(operationContext);
    }


    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            muleContext.getInjector().inject(executorDelegate);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(
                    createStaticMessage("Could not perform dependency injection on operation class " + executorDelegate.getClass().getName()), e, this);
        }

        initialiseIfNeeded(executorDelegate);
    }

    @Override
    public void start() throws MuleException
    {
        startIfNeeded(executorDelegate);
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(executorDelegate);
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(executorDelegate, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
        if (executorDelegate instanceof MuleContextAware)
        {
            ((MuleContextAware) executorDelegate).setMuleContext(context);
        }
    }
}
