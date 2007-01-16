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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is useful for loading resources and classes in a fault tolerant manner
 * that works across different applications servers. The resource and classloading
 * methods are SecurityManager friendly.
 */
// @ThreadSafe
public class ClassUtils extends org.apache.commons.lang.ClassUtils
{
    public static final Object[] NO_ARGS = new Object[]{};

    private static final Map wrapperToPrimitiveMap = new HashMap();
    static
    {
        wrapperToPrimitiveMap.put(Boolean.class, Boolean.TYPE);
        wrapperToPrimitiveMap.put(Byte.class, Byte.TYPE);
        wrapperToPrimitiveMap.put(Character.class, Character.TYPE);
        wrapperToPrimitiveMap.put(Short.class, Short.TYPE);
        wrapperToPrimitiveMap.put(Integer.class, Integer.TYPE);
        wrapperToPrimitiveMap.put(Long.class, Long.TYPE);
        wrapperToPrimitiveMap.put(Double.class, Double.TYPE);
        wrapperToPrimitiveMap.put(Float.class, Float.TYPE);
        wrapperToPrimitiveMap.put(Void.TYPE, Void.TYPE);
    }

    public static boolean isConcrete(Class clazz)
    {
        if (clazz == null)
        {
            throw new NullPointerException("clazz");
        }
        return !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()));
    }

    /**
     * Load a given resource. <p/> This method will try to load the resource using
     * the following methods (in order):
     * <ul>
     * <li>From
     * {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>From
     * {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     * 
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static URL getResource(final String resourceName, final Class callingClass)
    {
        URL url = (URL)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return Thread.currentThread().getContextClassLoader().getResource(resourceName);
            }
        });

        if (url == null)
        {
            url = (URL)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return ClassUtils.class.getClassLoader().getResource(resourceName);
                }
            });
        }

        if (url == null)
        {
            url = (URL)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return callingClass.getClassLoader().getResource(resourceName);
                }
            });
        }

        return url;
    }

    public static Enumeration getResources(final String resourceName, final Class callingClass)
    {
        Enumeration enumeration = (Enumeration)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return Thread.currentThread().getContextClassLoader().getResources(resourceName);
                }
                catch (IOException e)
                {
                    return null;
                }
            }
        });

        if (enumeration == null)
        {
            enumeration = (Enumeration)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return ClassUtils.class.getClassLoader().getResources(resourceName);
                    }
                    catch (IOException e)
                    {
                        return null;
                    }
                }
            });
        }

        if (enumeration == null)
        {
            enumeration = (Enumeration)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return callingClass.getClassLoader().getResources(resourceName);
                    }
                    catch (IOException e)
                    {
                        return null;
                    }
                }
            });
        }

        return enumeration;
    }

    /**
     * Load a class with a given name. <p/> It will try to load the class in the
     * following order:
     * <ul>
     * <li>From
     * {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>Using the basic {@link Class#forName(java.lang.String) }
     * <li>From
     * {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     * 
     * @param className The name of the class to load
     * @param callingClass The Class object of the calling object
     * @throws ClassNotFoundException If the class cannot be found anywhere.
     */
    public static Class loadClass(final String className, final Class callingClass)
        throws ClassNotFoundException
    {
        Class clazz = (Class)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return Thread.currentThread().getContextClassLoader().loadClass(className);
                }
                catch (ClassNotFoundException e)
                {
                    return null;
                }
            }
        });

        if (clazz == null)
        {
            clazz = (Class)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return Class.forName(className);
                    }
                    catch (ClassNotFoundException e)
                    {
                        return null;
                    }
                }
            });
        }

        if (clazz == null)
        {
            clazz = (Class)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return ClassUtils.class.getClassLoader().loadClass(className);
                    }
                    catch (ClassNotFoundException e)
                    {
                        return null;
                    }
                }
            });
        }

        if (clazz == null)
        {
            clazz = (Class)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return callingClass.getClassLoader().loadClass(className);
                    }
                    catch (ClassNotFoundException e)
                    {
                        return null;
                    }
                }
            });
        }

        if (clazz == null)
        {
            throw new ClassNotFoundException(className);
        }

        return clazz;
    }

    /**
     * Prints the current classloader hierarchy - useful for debugging.
     */
    public static void printClassLoader()
    {
        System.out.println("ClassLoaderUtils.printClassLoader");
        printClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Prints the classloader hierarchy from a given classloader - useful for
     * debugging.
     */
    public static void printClassLoader(ClassLoader cl)
    {
        System.out.println("ClassLoaderUtils.printClassLoader(cl = " + cl + ")");

        if (cl != null)
        {
            printClassLoader(cl.getParent());
        }
    }

    public static Object instanciateClass(Class clazz, Object[] constructorArgs)
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Class[] args;
        if (constructorArgs != null)
        {
            args = new Class[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++)
            {
                if (constructorArgs[i] == null)
                {
                    args[i] = null;
                }
                else
                {
                    args[i] = constructorArgs[i].getClass();
                }
            }
        }
        else
        {
            args = new Class[0];
        }

        // try the arguments as given
        Constructor ctor = getConstructor(clazz, args);

        if (ctor == null)
        {
            // try again but adapt value classes to primitives
            ctor = getConstructor(clazz, wrappersToPrimitives(args));
        }

        if (ctor == null)
        {
            StringBuffer argsString = new StringBuffer(100);
            for (int i = 0; i < args.length; i++)
            {
                argsString.append(args[i].getName()).append(", ");
            }
            throw new NoSuchMethodException("could not find constructor with matching arg params: "
                            + argsString);
        }

        return ctor.newInstance(constructorArgs);
    }

    public static Object instanciateClass(String name, Object[] constructorArgs)
        throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
        InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class clazz = loadClass(name, ClassUtils.class);
        return instanciateClass(clazz, constructorArgs);

    }

    public static Object instanciateClass(String name, Object[] constructorArgs, Class callingClass)
        throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
        InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class clazz = loadClass(name, callingClass);
        return instanciateClass(clazz, constructorArgs);

    }

    public static Class[] getParameterTypes(Object bean, String methodName)
    {
        if (!methodName.startsWith("set"))
        {
            methodName = "set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
        }
        Method methods[] = bean.getClass().getMethods();

        for (int i = 0; i < methods.length; i++)
        {
            if (methods[i].getName().equals(methodName))
            {
                return methods[i].getParameterTypes();
            }
        }
        return new Class[]{};
    }

    /**
     * Returns a matching method for the given name and parameters on the given class
     * If the parameterTypes arguments is null it will return the first matching
     * method on the class
     * 
     * @param name the method name to find
     * @param parameterTypes an array of argument types or null
     * @param clazz the class to find the method on
     * @return the Method object or null if none was found
     */
    public static Method getMethod(String name, Class[] parameterTypes, Class clazz)
    {
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            if (methods[i].getName().equals(name))
            {
                if (parameterTypes == null)
                {
                    return methods[i];
                }
                else if (Arrays.equals(methods[i].getParameterTypes(), parameterTypes))
                {
                    return methods[i];
                }
            }
        }
        return null;
    }

    public static Constructor getConstructor(Class clazz, Class[] paramTypes)
    {
        Constructor[] ctors = clazz.getConstructors();
        for (int i = 0; i < ctors.length; i++)
        {
            if (ctors[i].getParameterTypes().length == paramTypes.length)
            {
                Class[] types = ctors[i].getParameterTypes();
                boolean match = true;
                for (int x = 0; x < types.length; x++)
                {
                    if (paramTypes[x] == null)
                    {
                        match = true;
                    }
                    else
                    {
                        match = types[x].isAssignableFrom(paramTypes[x]);
                    }
                }
                if (match)
                {
                    return ctors[i];
                }
            }
        }
        return null;
    }

    /**
     * A helper method that will find all matching methods on a class with the given
     * parameter type
     * 
     * @param implementation the class to build methods on
     * @param parameterTypes the argument param types to look for
     * @param voidOk whether void methods shouldbe included in the found list
     * @param matchOnObject Determines whether parameters of OBject type are matched
     *            when they are of Object.class type
     * @param ignoreMethods An array of method names to ignore. Often 'equals' is not
     *            a desired match. This argument can be null.
     * @return a list of methods on the class that match the criteria. If there are
     *         none, an empty list is returned
     */
    public static List getSatisfiableMethods(Class implementation,
                                             Class[] parameterTypes,
                                             boolean voidOk,
                                             boolean matchOnObject,
                                             String[] ignoreMethods)
    {
        List result = new ArrayList();
        List ignore = (ignoreMethods == null ? Collections.EMPTY_LIST : Arrays.asList(ignoreMethods));
        List methods = Arrays.asList(implementation.getMethods());
        for (Iterator iterator = methods.iterator(); iterator.hasNext();)
        {
            Method method = (Method)iterator.next();
            Class[] methodParams = method.getParameterTypes();

            if (compare(methodParams, parameterTypes, matchOnObject))
            {
                if (!ignore.contains(method.getName()))
                {
                    if ((method.getReturnType().getName().equals("void") && voidOk)
                                    || !method.getReturnType().getName().equals("void"))
                    {
                        result.add(method);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Can be used by serice endpoints to select which service to use based on what's
     * loaded on the classpath
     * 
     * @param className The class name to look for
     * @param currentClass the calling class
     * @return true if the class is on the path
     */
    public static boolean isClassOnPath(String className, Class currentClass)
    {
        try
        {
            return (loadClass(className, currentClass) != null);
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    /**
     * Used for creating an array of class types for an array or single object
     * 
     * @param object single object or array
     * @return an array of class types for the object
     */
    public static Class[] getClassTypes(Object object)
    {
        Class[] types;
        if (object instanceof Object[])
        {
            Object[] objects = (Object[])object;
            types = new Class[objects.length];
            for (int i = 0; i < objects.length; i++)
            {
                types[i] = objects[i].getClass();
            }
        }
        else
        {
            types = new Class[1];
            types[0] = object.getClass();
        }
        return types;
    }

    public static String getClassName(Class clazz)
    {
        if (clazz == null)
        {
            return null;
        }
        String name = clazz.getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    public static boolean compare(Class[] c1, Class[] c2, boolean matchOnObject)
    {
        if (c1.length != c2.length)
        {
            return false;
        }
        for (int i = 0; i < c1.length; i++)
        {
            if (c1[i].equals(Object.class) && !matchOnObject)
            {
                return false;
            }
            if (!c1[i].isAssignableFrom(c2[i]))
            {

                return false;
            }
        }
        return true;
    }

    public static Class wrapperToPrimitive(Class wrapper)
    {
        return (Class)MapUtils.getObject(wrapperToPrimitiveMap, wrapper, wrapper);
    }

    public static Class[] wrappersToPrimitives(Class[] wrappers)
    {
        if (wrappers == null)
        {
            return null;
        }

        if (wrappers.length == 0)
        {
            return wrappers;
        }

        Class[] primitives = new Class[wrappers.length];

        for (int i = 0; i < wrappers.length; i++)
        {
            primitives[i] = (Class)MapUtils.getObject(wrapperToPrimitiveMap, wrappers[i], wrappers[i]);
        }

        return primitives;
    }

}
