/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.util;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides utilities to work with reflection.
 */
public class ReflectionUtils
{

    private ReflectionUtils()
    {
    }

    /**
     * Returns the list of interfaces implemented in a given class.
     *
     * @param aClass class to analyze. Non null.
     * @return the list of interfaces implemented in the provided class and all its super classes.
     */
    public static Class<?>[] findImplementedInterfaces(Class<?> aClass)
    {
        checkArgument(aClass != null, "Class to analyze cannot be null");

        Class<?> currentClass = aClass;
        List<Class<?>> foundInterfaces = new LinkedList<>();
        while (currentClass != null)
        {
            Class<?>[] interfaces = currentClass.getInterfaces();
            Collections.addAll(foundInterfaces, interfaces);
            currentClass = currentClass.getSuperclass();
        }

        return foundInterfaces.toArray(new Class<?>[0]);
    }
}
