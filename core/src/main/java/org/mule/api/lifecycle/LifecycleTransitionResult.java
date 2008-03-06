/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.lifecycle;

import org.mule.util.ClassUtils;
import org.mule.config.i18n.CoreMessages;
import org.mule.api.MuleException;

import java.util.Iterator;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Restrict possible results - only OK or a retry based on some throwable are currently allowed.
 */
public final class LifecycleTransitionResult
{

    public static final String OK_NAME = "ok";
    public static final String RETRY_NAME = "retry";


    /** Transition successful **/
    public static final LifecycleTransitionResult OK = new LifecycleTransitionResult(OK_NAME, true, null, null);

    private boolean ok;
    private Throwable throwable;
    private Object target;
    private String name;

    private LifecycleTransitionResult(String name, boolean ok, Throwable throwable, Object target)
    {
        this.name = name;
        this.ok = ok;
        this.throwable = throwable;
        this.target = target;
    }

    public static LifecycleTransitionResult retry(Throwable throwable, Object target)
    {
        return new LifecycleTransitionResult(RETRY_NAME, false, throwable, target);
    }

    public boolean isOk()
    {
        return ok;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public String toString()
    {
        return ClassUtils.getSimpleName(getClass()) + ": " + name;
    }

    public LifecycleException nestedRetryLifecycleException()
    {
        return (LifecycleException)
                new LifecycleException(CoreMessages.nestedRetry(), target).initCause(throwable);
    }

    public InitialisationException nestedRetryInitialisationException()
    {
        return (InitialisationException)
                new InitialisationException(CoreMessages.nestedRetry(), (Initialisable) target).initCause(throwable);
    }





    /**
     * The logic for processing a collection of children
     *
     * @param iface The lifecycle interface to be called
     * @param objects An iterator over all children that must also be called
     * @return {@link LifecycleTransitionResult#OK} if all succeed
     * @throws org.mule.api.lifecycle.LifecycleException if any fail
     */
    private static LifecycleTransitionResult processAllNoRetry(Class iface, Iterator objects) throws LifecycleException
    {
        if (!iface.isAssignableFrom(Lifecycle.class))
        {
            throw new IllegalArgumentException("Not a Lifecycle interface: " + iface);
        }

        // all interfaces have a single method
        Method method = iface.getMethods()[0];
        // some interfaces have a single exception, others none
        boolean hasException = method.getExceptionTypes().length > 0;
        Class exception = hasException ? method.getExceptionTypes()[0] : null;

        while (objects.hasNext())
        {
            Object target = objects.next();
            processSingleNoRetry(target, method, exception, iface);
        }
        return OK;
    }

    private static LifecycleTransitionResult processSingleNoRetry(Object target, Method method, Class exception, Class iface)
            throws LifecycleException
    {
        if (! iface.isAssignableFrom(target.getClass()))
        {
            throw new IllegalArgumentException(ClassUtils.getSimpleName(target.getClass()) +
                    " is not an " + ClassUtils.getSimpleName(iface));
        }
        try
        {
            LifecycleTransitionResult result = (LifecycleTransitionResult) method.invoke(target, ClassUtils.NO_ARGS);
            if (!isOk(result))
            {
                if (null != exception)
                {
                    if (exception.equals(InitialisationException.class))
                    {
                        throw result.nestedRetryInitialisationException();
                    }
                    else
                    {
                        throw result.nestedRetryLifecycleException();
                    }
                }
                else
                {
                    IllegalStateException error = new IllegalStateException("Unexpected state from transition: " + result);
                    error.initCause(result.getThrowable());
                    throw error;
                }
            }
            return result;
        }
        catch (IllegalAccessException e)
        {
            throw (IllegalArgumentException) new IllegalArgumentException("Unsupported interface: " + iface).initCause(e);
        }
        catch (InvocationTargetException e)
        {
            throw (IllegalArgumentException) new IllegalArgumentException("Unsupported interface: " + iface).initCause(e);
        }
    }

    /**
     * Initialise children
     */
    public static LifecycleTransitionResult initialiseAll(Iterator children) throws InitialisationException
    {
        try
        {
            return processAllNoRetry(Initialisable.class, children);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (LifecycleException e)
        {
            throw (IllegalStateException) new IllegalStateException("Unexpected exception: " + e).initCause(e);
        }
    }

    /**
     * Initialise parent and children
     */
    public static LifecycleTransitionResult initialiseAll(LifecycleTransitionResult status, final Iterator children)
            throws InitialisationException
    {
        return initialiseAll(status, new Closure()
        {
            public LifecycleTransitionResult doContinue() throws LifecycleException
            {
                return initialiseAll(children);
            }
        });
    }

    /**
     * Handle arbitrary processing that is conditional on first calling something else
     */
    public static LifecycleTransitionResult initialiseAll(LifecycleTransitionResult status, Closure rest)
            throws InitialisationException
    {
        if (isOk(status))
        {
            try
            {
                return rest.doContinue();
            }
            catch (InitialisationException e)
            {
                throw e;
            }
            catch (MuleException e)
            {
                throw (IllegalStateException) new IllegalStateException("Unexpected exception: " + e).initCause(e);
            }
        }
        else
        {
            throw status.nestedRetryInitialisationException();
        }
    }

    public static LifecycleTransitionResult startOrStopAll(LifecycleTransitionResult status, Closure rest)
            throws MuleException
    {
        if (isOk(status))
        {
            return rest.doContinue();
        }
        else
        {
            throw status.nestedRetryLifecycleException();
        }
    }

    public static boolean isOk(LifecycleTransitionResult result)
    {
        return null != result && result.isOk();
    }

    public static interface Closure
    {
        LifecycleTransitionResult doContinue() throws MuleException;
    }

}
