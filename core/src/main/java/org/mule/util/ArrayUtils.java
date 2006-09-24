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

import java.lang.reflect.Array;

// @Immutable
public class ArrayUtils extends org.apache.commons.lang.ArrayUtils
{

    /**
     * Creates a copy of the given array, but with the given <code>Class</code> as
     * element type. Useful for arrays of objects that implement multiple interfaces
     * and a "typed view" onto these objects is required.
     * 
     * @param objects the array of objects
     * @param clazz the desired component type of the new array
     * @return <code>null</code> when objects is <code>null</code>, or a new
     *         array containing the elements of the source array which is typed to
     *         the given <code>clazz</code> parameter. If <code>clazz</code> is
     *         already the component type of the source array, the source array is
     *         returned (i.e. no copy is created).
     * @throws IllegalArgumentException if the <code>clazz</code> argument is
     *             <code>null</code>.
     * @throws ArrayStoreException if the elements in <code>objects</code> cannot
     *             be cast to <code>clazz</code>.
     */
    public static Object[] toArrayOfComponentType(Object[] objects, Class clazz)
    {
        if (objects == null || objects.getClass().getComponentType().equals(clazz))
        {
            return objects;
        }

        if (clazz == null)
        {
            throw new IllegalArgumentException("Class must not be null!");
        }

        Object[] result = (Object[])Array.newInstance(clazz, objects.length);
        System.arraycopy(objects, 0, result, 0, objects.length);
        return result;
    }

}
