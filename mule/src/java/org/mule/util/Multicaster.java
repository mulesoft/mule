package org.mule.util;

/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <code>Multicaster</code> is a utility that can call a given method on a
 * collection of objects that implement one or more common interfaces. Thecreate
 * method returns a proxy that can be cast to any of the the interfaces passed
 * and be used like a single object.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Multicaster
{
    public static Object create(Class theInterface, Collection objects)
    {
        return create(new Class[] { theInterface }, objects);
    }

    public static Object create(Class theInterface, Collection objects, InvokeListener listener)
    {
        return create(new Class[] { theInterface }, objects, listener);
    }

    public static Object create(Class[] interfaces, Collection objects)
    {
        return create(interfaces, objects, null);
    }

    public static Object create(Class[] interfaces, Collection objects, InvokeListener listener)
    {
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                              interfaces,
                                              new CastingHandler(objects, listener));

        return proxy;
    }

    private static class CastingHandler implements InvocationHandler
    {
        private Collection objects;
        private InvokeListener listener;

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
            Object result = null;
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                try {
                    item = iterator.next();
                    result = method.invoke(item, args);
                    if (listener != null) {
                        listener.afterExecute(item, method, args);
                    }
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Throwable t) {
                    if (listener != null) {
                        t = listener.onException(item, method, args, t);
                        if (t != null) {
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

        /**
         * 
         * @param object
         * @param method
         * @param args
         * @param t
         * @return A Throwable to throw otherwise null to ingore the exception
         */
        Throwable onException(Object object, Method method, Object[] args, Throwable t);
    }
}
