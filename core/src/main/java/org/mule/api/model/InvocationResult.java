/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.model;

import org.mule.config.i18n.CoreMessages;

import java.lang.reflect.Method;

/**
 * Tracks the state of an invocation on a component using an {@link EntryPointResolver}
 */
public class InvocationResult
{
    public static enum State
    {
        /** the resolver performing the invocation knows that it cannot attempt to make the invocation */
        NOT_SUPPORTED,

        /** the invocation was successful */
        SUCCESSFUL,

        /** The invocation was attempted but failed */
        FAILED
    }
    @Deprecated
    public static final State STATE_INVOKE_NOT_SUPPORTED = State.NOT_SUPPORTED;

    @Deprecated
    public static final State STATE_INVOKED_SUCESSFUL = State.SUCCESSFUL;

    @Deprecated
    public static final State STATE_INVOKED_FAILED = State.FAILED;

    private String errorMessage;

    /** the name of the method called for this invocation */
    private String methodCalled;

    /** the result of calling the invocation method */
    private Object result;

    /** the state of this invocation */
    private State state;

    /** The entry-point resolver that created this InvocationResult */
    private EntryPointResolver resolver;

    /**
     * Will construct an InvocationResult with a given state. The state must be either
     * {@link org.mule.api.model.InvocationResult.State#NOT_SUPPORTED} if the resolver performing the invocation knows that it cannot
     * attempt to make the invocation
     * {@link org.mule.api.model.InvocationResult.State#FAILED} If an invocation attempt is made but fails
     * {@link org.mule.api.model.InvocationResult.State#SUCCESSFUL} If the invocation was successful
     *
     * Typically, this constructor is used when the state is {@link org.mule.api.model.InvocationResult.State#NOT_SUPPORTED} or {@link org.mule.api.model.InvocationResult.State#FAILED}
     *
     * @param resolver the resolver being used to make the invocation
     * @param state the state of the result
     */
    public InvocationResult(EntryPointResolver resolver, State state)
    {
        this.state = state;
        this.resolver = resolver;
    }

    /**
     * Creates a result with the result payload set. The state of this result will be {@link org.mule.api.model.InvocationResult.State#SUCCESSFUL}
     * since only in this state will a result be set.
     *
     * @param resolver the resolver being used to make the invocation
     * @param result the result of a successful invocation
     * @param method the method invoke by this invocation
     */
    public InvocationResult(EntryPointResolver resolver, Object result, Method method)
    {

        this.result = result;
        this.state = State.SUCCESSFUL;
        this.methodCalled = method.getName();
        this.resolver = resolver;
    }

    /**
     * Returns the name of the method invoked, this property is only set if the state of the invocation is
     * {@link org.mule.api.model.InvocationResult.State#SUCCESSFUL}
     *
     * @return the name of the method invoked
     */
    public String getMethodCalled()
    {
        return methodCalled;
    }

    /**
     * The result of this invocation
     *
     * @return an object or null if the result did not yield a result or because the state of this invocation result
     *         is either {@link org.mule.api.model.InvocationResult.State#NOT_SUPPORTED} or {@link org.mule.api.model.InvocationResult.State#FAILED}.
     */
    public Object getResult()
    {
        return result;
    }

    /**
     * @return the state of this invocation. Possible values are:
     * {@link org.mule.api.model.InvocationResult.State#NOT_SUPPORTED} if the resolver performing the invocation knows that it cannot
     * attempt to make the invocation
     * {@link org.mule.api.model.InvocationResult.State#FAILED} If an invocation attempt is made but fails
     * {@link org.mule.api.model.InvocationResult.State#SUCCESSFUL} If the invocation was successful
     */
    public State getState()
    {
        return state;
    }

    /**
     * An optional error message can be set if the invocation state is not {@link org.mule.api.model.InvocationResult.State#SUCCESSFUL}
     *
     * @param message the error message
     */
    public void setErrorMessage(String message)
    {
        if (state == State.SUCCESSFUL)
        {
            throw new IllegalStateException(CoreMessages.invocationSuccessfulCantSetError().toString());
        }
        errorMessage = message;
    }

    /**
     * Returns true if an error message has been set on this result, false otherwise
     *
     * @return true if an error message has been set on this result, false otherwise
     */
    public boolean hasError()
    {
        return errorMessage != null;
    }

    /**
     * Returns the error message set on this result or null if none has been set
     *
     * @return the error message set on this result or null if none has been set
     */
    public String getErrorMessage()
    {
        return (errorMessage==null ? null : resolver.getClass().getSimpleName() + ": " + errorMessage);
    }

    public void setErrorTooManyMatchingMethods(Object component, Class<?>[] argTypes, String methods)
    {
        setErrorMessage(CoreMessages.tooManyAcceptableMethodsOnObjectUsingResolverForTypes(
                component.getClass().getName(), argTypes, methods).toString());
    }

    public void setErrorNoMatchingMethods(Object component, Class<?>[] args)
    {
        setErrorMessage(CoreMessages.noEntryPointFoundWithArgsUsingResolver(
                component.getClass().getName(), args).toString());
    }

    public void setErrorNoMatchingMethodsCalled(Object component, String methods)
    {
        setErrorMessage(CoreMessages.noMatchingMethodsOnObjectCalledUsingResolver(
                component.getClass().getName(), methods).toString());
    }

}
