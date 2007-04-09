/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <code>Multicaster</code> is a utility that can call a given method on a
 * collection of objects that implement one or more common interfaces. The create
 * method returns a proxy that can be cast to any of the the interfaces passed and be
 * used like a single object.
 */
// @ThreadSafe
public final class Multicaster
{
    /** Do not instanciate. */
    private Multicaster ()
    {
        // no-op
    }

    public static Object create(Class theInterface, Collection objects)
    {
        return create(new Class[]{theInterface}, objects);
    }

    public static Object create(Class theInterface, Collection objects, InvokeListener listener)
    {
        return create(new Class[]{theInterface}, objects, listener);
    }

    public static Object create(Class[] interfaces, Collection objects)
    {
        return create(interfaces, objects, null);
    }

    public static Object create(Class[] interfaces, Collection objects, InvokeListener listener)
    {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces,
            new CastingHandler(objects, listener));
    }

    private static class CastingHandler implements InvocationHandler
    {
        private final Collection objects;
        private final InvokeListener listener;

        public CastingHandler(Collection objects)
        {
            this(objects, null);
        }

        public CastingHandler(Collection objects, InvokeListener listener)
        {
            this.objects = objects;
            this.listener = listener;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            List results = new ArrayList();
            Object item = null;
            Object result;

            for (Iterator iterator = objects.iterator(); iterator.hasNext();)
            {
                try
                {
                    item = iterator.next();
                    result = method.invoke(item, args);
                    if (listener != null)
                    {
                        listener.afterExecute(item, method, args);
                    }
                    if (result != null)
                    {
                        results.add(result);
                    }
                }
                catch (Throwable t)
                {
                    // TODO MULE-863: What should we do if null?
                    if (listener != null)
                    {
                        t = listener.onException(item, method, args, t);
                        if (t != null)
                        {
                            throw t;
                        }
                    }
                }
            }
            return results;
        }
    }

    public static interface InvokeListener
    {
        void afterExecute(Object object, Method method, Object[] args);

        Throwable onException(Object object, Method method, Object[] args, Throwable t);
    }

}
