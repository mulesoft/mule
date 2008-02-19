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

import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.api.MuleException;

import java.util.Iterator;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * These utilities abstract out the client logic for processing nested calls to the same interface.
 * So, for example, if a component needs to initialise several sub-components, it can use the
 * routines here to do so.  This keeps code that depends on the enumeration in
 * {@link org.mule.api.lifecycle.LifecycleTransitionResult} in a single place.
 */
public class LifecycleLogic
{

    /**
     * The logic for processing a collection of children
     *
     * @param iface The lifecycle interface to be called
     * @param objects An iterator over all children that must also be called
     * @return {@link org.mule.api.lifecycle.LifecycleTransitionResult#OK} if all succeed
     * @throws LifecycleException if any fail
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
        return LifecycleTransitionResult.OK;
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
            if (result != LifecycleTransitionResult.OK)
            {
                if (null != exception)
                {
                    if (exception.equals(InitialisationException.class))
                    {
                        throw new InitialisationException(CoreMessages.nestedRetry(), (Initialisable) target);
                    }
                    else
                    {
                        throw new LifecycleException(CoreMessages.nestedRetry(), target);
                    }
                }
                else
                {
                    throw new IllegalStateException("Unexpected state from transition: " + result);
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
    public static LifecycleTransitionResult initialiseAll(Initialisable component, LifecycleTransitionResult status,
                                                          final Iterator children) throws InitialisationException
    {
        return initialiseAll(component, status, new Closure()
        {
            public LifecycleTransitionResult doContinue() throws InitialisationException
            {
                return initialiseAll(children);
            }
        });
    }

    /**
     * Handle arbitrary processing that is conditional on first calling something else
     */
    public static LifecycleTransitionResult initialiseAll(Initialisable component, LifecycleTransitionResult status,
                                                          Closure rest) throws InitialisationException
    {
        if (status == LifecycleTransitionResult.OK)
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
            throw new InitialisationException(CoreMessages.nestedRetry(), component);
        }
    }

    public static LifecycleTransitionResult startAll(Startable component, LifecycleTransitionResult status,
                                                     Closure rest) throws MuleException
    {
        if (status == LifecycleTransitionResult.OK)
        {
            return rest.doContinue();
        }
        else
        {
            throw new LifecycleException(CoreMessages.nestedRetry(), component);
        }
    }

    public static LifecycleTransitionResult stopAll(Stoppable component, LifecycleTransitionResult status,
                                                     Closure rest) throws MuleException
    {
        if (status == LifecycleTransitionResult.OK)
        {
            return rest.doContinue();
        }
        else
        {
            throw new LifecycleException(CoreMessages.nestedRetry(), component);
        }
    }


    public static interface Closure
    {
        LifecycleTransitionResult doContinue() throws MuleException;
    }

}
