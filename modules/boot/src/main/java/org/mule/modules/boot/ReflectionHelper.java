/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import org.mule.MuleServer;
import org.mule.util.ClassUtils;
import org.mule.util.SystemUtils;

import java.lang.reflect.Method;
import java.net.URL;

import org.tanukisoftware.wrapper.WrapperSimpleApp;

/**
 * JRockit VM has problems with MuleBootstrap's approach. Namely, for jars loaded
 * dynamically into a system classloader (launcher) it can't resolve static method
 * calls. At the same time, inspecting the dynamically loaded class reports all methods
 * as available. The class implements all such calls via reflection as a workaround.
 * Sun's VM had no problems with the original approach, and doesn't have any with this
 * workaround either.
 */
public class ReflectionHelper
{

    /** Do not instantiate ReflectionHelper. */
    private ReflectionHelper()
    {
        // no-op
    }

    /**
     * Wrap {@link ClassUtils#getResource(String, Class)}.
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static URL getResource(final String resourceName, final Class callingClass) throws Exception
    {
        Class clazz = ClassUtils.class;
        Method m = clazz.getMethod("getResource", new Class[] {String.class, Class.class});
        Object result = m.invoke(null, new Object[] { resourceName, callingClass});
        return (URL) result;
    }

    /**
     * Wrap {@link ClassUtils#isClassOnPath(String, Class)}.
     * @param className The class name to look for
     * @param currentClass the calling class
     * @return true if the class is on the path
     */
    public static boolean isClassOnPath(String className, Class currentClass) throws Exception
    {
        Class clazz = ClassUtils.class;
        Method m = clazz.getMethod("isClassOnPath", new Class[] {String.class, Class.class});
        Object result = m.invoke(null, new Object[] { className, currentClass});
        return ((Boolean) result).booleanValue();
    }

    /**
     * Workaround for JRockit unable to access a public static field value.
     * @return value of {@link MuleServer#CLI_OPTIONS}
     */
    public static String[][] getCliOptions() throws Exception
    {
        return (String[][]) MuleServer.class.getField("CLI_OPTIONS").get(null);
    }

    /**
     * Wrap {@link SystemUtils#getCommandLineOption(String, String[], String[][])}.
     */
    public static String getCommandLineOption(String option, String args[], String opts[][]) throws Exception
    {
        Class clazz = SystemUtils.class;
        Method m = clazz.getMethod("getCommandLineOption", new Class[] {String.class, String[].class, String[][].class});
        Object result = m.invoke(null, new Object[] { option, args, opts});
        return (String) result;
    }

    /**
     * Wrap {@link WrapperSimpleApp#main(String[])}.
     */
    public static void wrapperMain(String[] args) throws Exception
    {
        Class clazz = WrapperSimpleApp.class;
        Method m = clazz.getMethod("main", new Class[] {String[].class});
        m.invoke(null, new Object[] {args});
    }

    /**
     * Wrap {@link WrapperSimpleApp#stop(int)}.
     */
    public static void wrapperStop(int exitCode) throws Exception
    {
        Class clazz = WrapperSimpleApp.class;
        Method m = clazz.getMethod("stop", new Class[] {int.class});
        m.invoke(null, new Object[] {new Integer(exitCode)});
    }

}
