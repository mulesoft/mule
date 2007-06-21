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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extend the Apache Commons ClassUtils to provide additional functionality.
 *
 * <p>This class is useful for loading resources and classes in a fault tolerant manner
 * that works across different applications servers. The resource and classloading
 * methods are SecurityManager friendly.</p>
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
            throw new IllegalArgumentException("clazz may not be null");
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
        URL url = (URL) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return Thread.currentThread().getContextClassLoader().getResource(resourceName);
            }
        });

        if (url == null)
        {
            url = (URL) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    return ClassUtils.class.getClassLoader().getResource(resourceName);
                }
            });
        }

        if (url == null)
        {
            url = (URL) AccessController.doPrivileged(new PrivilegedAction()
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
        Enumeration enumeration = (Enumeration) AccessController.doPrivileged(new PrivilegedAction()
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
            enumeration = (Enumeration) AccessController.doPrivileged(new PrivilegedAction()
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
            enumeration = (Enumeration) AccessController.doPrivileged(new PrivilegedAction()
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
        Class clazz = (Class) AccessController.doPrivileged(new PrivilegedAction()
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
            clazz = (Class) AccessController.doPrivileged(new PrivilegedAction()
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
            clazz = (Class) AccessController.doPrivileged(new PrivilegedAction()
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
            clazz = (Class) AccessController.doPrivileged(new PrivilegedAction()
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
     * method on the class.
     * 
     * @param clazz the class to find the method on
     * @param name the method name to find
     * @param parameterTypes an array of argument types or null
     * @return the Method object or null if none was found
     */
    public static Method getMethod(Class clazz, String name, Class[] parameterTypes)
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
                else if (compare(methods[i].getParameterTypes(), parameterTypes, true))
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
            Class[] types = ctors[i].getParameterTypes();
            if (types.length == paramTypes.length)
            {
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
     * @param matchOnObject determines whether parameters of OBject type are matched
     *            when they are of Object.class type
     * @param ignoredMethodNames a Set of method names to ignore. Often 'equals' is
     *            not a desired match. This argument can be null.
     * @return a List of methods on the class that match the criteria. If there are
     *         none, an empty list is returned
     */
    public static List getSatisfiableMethods(Class implementation,
                                             Class[] parameterTypes,
                                             boolean voidOk,
                                             boolean matchOnObject,
                                             Set ignoredMethodNames)
    {
        List result = new ArrayList();

        if (ignoredMethodNames == null)
        {
            ignoredMethodNames = Collections.EMPTY_SET;
        }

        Method[] methods = implementation.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            Class[] methodParams = method.getParameterTypes();

            if (compare(methodParams, parameterTypes, matchOnObject))
            {
                if (!ignoredMethodNames.contains(method.getName()))
                {
                    String returnType = method.getReturnType().getName();
                    if ((returnType.equals("void") && voidOk) || !returnType.equals("void"))
                    {
                        result.add(method);
                    }
                }
            }
        }

        return result;
    }

    public static List getSatisfiableMethodsWithReturnType(Class implementation,
                                             Class returnType,
                                             boolean matchOnObject,
                                             Set ignoredMethodNames)
    {
        List result = new ArrayList();

        if (ignoredMethodNames == null)
        {
            ignoredMethodNames = Collections.EMPTY_SET;
        }

        Method[] methods = implementation.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            Class returns = method.getReturnType();

            if (compare(new Class[]{returns}, new Class[]{returnType}, matchOnObject))
            {
                if (!ignoredMethodNames.contains(method.getName()))
                {
                    result.add(method);
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

        // TODO MULE-1088: instead of returning the classes of an array's elements we should
        // just return the array class - which makes the whole method pointless!?
//        if (object.getClass().isArray())
//        {
//            types = new Class[]{object.getClass()};
//        }

        if (object instanceof Object[])
        {
            Object[] objects = (Object[]) object;
            types = new Class[objects.length];
            for (int i = 0; i < objects.length; i++)
            {
                types[i] = objects[i].getClass();
            }
        }
        else
        {
            types = new Class[]{object.getClass()};
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
        return (Class) MapUtils.getObject(wrapperToPrimitiveMap, wrapper, wrapper);
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
            primitives[i] = (Class) MapUtils.getObject(wrapperToPrimitiveMap, wrappers[i], wrappers[i]);
        }

        return primitives;
    }

    /**
     * Provide a simple-to-understand class name (with access to only Java 1.4 API).
     * @param clazz The class whose name we will generate
     * @return A readable name for the class
     */
    public static String getSimpleName(Class clazz)
    {
        if (null == clazz)
        {
            return "null";
        }
        else
        {
            return classNameHelper(new BufferedReader(new CharArrayReader(clazz.getName().toCharArray())));
        }
    }

    private static String classNameHelper(Reader encodedName)
    {
        // I did consider separating this data from the code, but I could not find a
        // solution that was as clear to read, or clearly motivated (these data are not
        // used elsewhere).

        try
        {
            encodedName.mark(1);
            switch(encodedName.read())
            {
                case -1: return "null";
                case 'Z': return "boolean";
                case 'B': return "byte";
                case 'C': return "char";
                case 'D': return "double";
                case 'F': return "float";
                case 'I': return "int";
                case 'J': return "long";
                case 'S': return "short";
                case '[': return classNameHelper(encodedName) + "[]";
                case 'L': return shorten(new BufferedReader(encodedName).readLine());
                default:
                    encodedName.reset();
                    return shorten(new BufferedReader(encodedName).readLine());
            }
        }
        catch (IOException e)
        {
            return "unknown type: " + e.getMessage();
        }
    }

    /**
     * @param clazz A class name (with possible package and trailing semicolon)
     * @return The short name for the class
     */
    private static String shorten(String clazz)
    {
        if (null != clazz && clazz.endsWith(";"))
        {
            clazz = clazz.substring(0, clazz.length() - 1);
        }
        if (null != clazz && clazz.lastIndexOf(".") > -1)
        {
            clazz = clazz.substring(clazz.lastIndexOf(".") + 1, clazz.length());
        }
        return clazz;
    }
}
