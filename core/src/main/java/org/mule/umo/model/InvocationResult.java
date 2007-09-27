/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.model;

import org.mule.config.i18n.CoreMessages;

/** TODO */
public class InvocationResult
{
    /** the resover performing the invocation knows that it cannot attempt to make the invocation */
    public static final int STATE_INVOKE_NOT_SUPPORTED = 0;

    /** the invocation was successful */
    public static final int STATE_INVOKED_SUCESSFUL = 1;

    /** The invocation was attempted but failed */
    public static final int STATE_INVOKED_FAILED = 2;

    private String errorMessage;

    private Object result;

    private int state;

    /**
     * Will construct an InvocationResult with a given state. The state must be either
     * {@link #STATE_INVOKE_NOT_SUPPORTED} if the resover performing the invocation knows that it cannot
     * attempt to make the invocation
     * {@link #STATE_INVOKED_FAILED} If an invocation attempt is made but fails
     * {@link #STATE_INVOKED_SUCESSFUL} If the invocation was successful
     *
     * @param state the state of the result
     */
    public InvocationResult(int state)
    {
        if (state < 0 || state > 2)
        {
            throw new IllegalArgumentException("state");
        }
        this.state = state;
    }

    /**
     * Creates a result with the result payload set. The state of this result will be {@link #STATE_INVOKED_SUCESSFUL}
     * since only in this state will a result be set.
     *
     * @param result the result of a successful invocation
     */
    public InvocationResult(Object result)
    {

        this.result = result;
        this.state = STATE_INVOKED_SUCESSFUL;
    }

    /**
     * The result of this invocation
     *
     * @return an object or null if the result did not yeild a result or because the state of this invocation result
     *         is either {@link #STATE_INVOKE_NOT_SUPPORTED} or {@link #STATE_INVOKED_FAILED}.
     */
    public Object getResult()
    {
        return result;
    }

    /**
     * Returns the state of this invocation. Possible values are:
     * {@link #STATE_INVOKE_NOT_SUPPORTED} if the resover performing the invocation knows that it cannot
     * attempt to make the invocation
     * {@link #STATE_INVOKED_FAILED} If an invocation attempt is made but fails
     * {@link #STATE_INVOKED_SUCESSFUL} If the invocation was successful
     *
     * @return
     */
    public int getState()
    {
        return state;
    }

    /**
     * An optional error message can be set if the invocation state is not {@link #STATE_INVOKED_SUCESSFUL}
     *
     * @param message
     */
    public void setErrorMessage(String message)
    {
        if (state == STATE_INVOKED_SUCESSFUL)
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
        return errorMessage;
    }

    public void setErrorTooManyMatchingMethods(Object component, Class[] argTypes, String methods, UMOEntryPointResolver resolver)
    {
        setErrorMessage(CoreMessages.tooManyAcceptableMethodsOnObjectUsingResolverForTypes(
                component.getClass().getName(), argTypes, resolver).toString());
    }

    public void setErrorTooManyMatchingMethods(Object component, Class[] argTypes, UMOEntryPointResolver resolver)
    {
        setErrorMessage(CoreMessages.tooManyAcceptableMethodsOnObjectUsingResolverForTypes(
                component.getClass().getName(), argTypes, resolver).toString());
    }

    public void setErrorNoMatchingMethods(Object component, Class[] args, UMOEntryPointResolver resolver)
    {
        setErrorMessage(CoreMessages.noEntryPointFoundWithArgsUsingResolver(
                component.getClass().getName(), args, resolver).toString());
    }

    public void setErrorNoMatchingMethodsCalled(Object component, String methods, UMOEntryPointResolver resolver)
    {
        setErrorMessage(CoreMessages.noMatchingMethodsOnObjectCalledUsingResolver(
                component.getClass().getName(), methods, resolver).toString());
    }

}
