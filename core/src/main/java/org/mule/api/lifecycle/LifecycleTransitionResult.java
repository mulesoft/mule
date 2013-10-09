/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Restrict possible results - only OK or a retry based on some throwable are currently allowed.
 */
public final class LifecycleTransitionResult
{
    /**
     * The logic for processing a collection of children
     *
     * @param iface The lifecycle interface to be called
     * @param objects An iterator over all children that must also be called
     * @throws org.mule.api.lifecycle.LifecycleException if any fail
     */
    private static void processAllNoRetry(Class<? extends Initialisable> iface, 
        Iterator<? extends Initialisable> objects) throws LifecycleException
    {
        if (!iface.isAssignableFrom(Lifecycle.class))
        {
            throw new IllegalArgumentException("Not a Lifecycle interface: " + iface);
        }

        // all interfaces have a single method
        Method method = iface.getMethods()[0];
        // some interfaces have a single exception, others none
        boolean hasException = method.getExceptionTypes().length > 0;
        Class<?> exception = hasException ? method.getExceptionTypes()[0] : null;

        while (objects.hasNext())
        {
            Object target = objects.next();
            processSingleNoRetry(target, method, exception, iface);
        }
    }

    private static void processSingleNoRetry(Object target, Method method, Class<?> exception, 
        Class<?> iface) throws LifecycleException
    {
        if (! iface.isAssignableFrom(target.getClass()))
        {
            throw new IllegalArgumentException(ClassUtils.getSimpleName(target.getClass()) +
                    " is not an " + ClassUtils.getSimpleName(iface));
        }
        try
        {
            method.invoke(target);
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

    public static void initialiseAll(Iterator<? extends Initialisable> children) throws InitialisationException
    {
        try
        {
            processAllNoRetry(Initialisable.class, children);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (LifecycleException e)
        {
            throw new IllegalStateException("Unexpected exception: ", e);
        }
    }
}
