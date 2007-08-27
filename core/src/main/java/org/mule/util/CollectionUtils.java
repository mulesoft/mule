/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;


// @ThreadSafe
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils
{

    /**
     * Creates an array of the given Collection's elements, but with the given
     * <code>Class</code> as element type. Useful for arrays of objects that
     * implement multiple interfaces and a "typed view" onto these objects is
     * required.
     * 
     * @param objects a Collection of objects
     * @param clazz the desired component type of the new array
     * @return <code>null</code> when objects is <code>null</code>, or a new
     *         array containing the elements of the source array which is typed to
     *         the given <code>clazz</code> parameter.
     * @throws IllegalArgumentException if the <code>clazz</code> argument is
     *             <code>null</code>.
     * @throws ArrayStoreException if the elements in <code>objects</code> cannot
     *             be cast to <code>clazz</code>.
     */
    public static Object[] toArrayOfComponentType(Collection objects, Class clazz)
    {
        if (objects == null)
        {
            return null;
        }

        if (clazz == null)
        {
            throw new IllegalArgumentException("Array target class must not be null");
        }

        if (objects.isEmpty())
        {
            return (Object[]) Array.newInstance(clazz, 0);
        }

        int i = 0, size = objects.size();
        Object[] result = (Object[]) Array.newInstance(clazz, size);
        Iterator iter = objects.iterator();

        while (i < size && iter.hasNext())
        {
            result[i++] = iter.next();
        }

        return result;
    }

    /**
     * Creates a String representation of the given Collection, with optional
     * newlines between elements. Class objects are represented by their full names.
     * 
     * @param c the Collection to format
     * @param newline indicates whether elements are to be split across lines
     * @return the formatted String
     */
    public static String toString(Collection c, boolean newline)
    {
        if (c == null || c.isEmpty())
        {
            return "[]";
        }

        return toString(c, c.size(), newline);
    }

    /**
     * Calls {@link #toString(Collection, int, boolean)} with <code>false</code>
     * for newline.
     */
    public static String toString(Collection c, int maxElements)
    {
        return toString(c, maxElements, false);
    }

    /**
     * Creates a String representation of the given Collection, with optional
     * newlines between elements. Class objects are represented by their full names.
     * Considers at most <code>maxElements</code> values; overflow is indicated by
     * an appended "[..]" ellipsis.
     * 
     * @param c the Collection to format
     * @param maxElements the maximum number of elements to take into account
     * @param newline indicates whether elements are to be split across lines
     * @return the formatted String
     */
    public static String toString(Collection c, int maxElements, boolean newline)
    {
        if (c == null || c.isEmpty())
        {
            return "[]";
        }

        int origNumElements = c.size();
        int numElements = Math.min(origNumElements, maxElements);
        boolean tooManyElements = (origNumElements > maxElements);

        StringBuffer buf = new StringBuffer(numElements * 32);
        buf.append('[');

        if (newline)
        {
            buf.append(SystemUtils.LINE_SEPARATOR);
        }

        Iterator items = c.iterator();
        for (int i = 0; i < numElements - 1; i++)
        {
            Object item = items.next();

            if (item instanceof Class)
            {
                buf.append(((Class) item).getName());
            }
            else
            {
                buf.append(item);
            }

            if (newline)
            {
                buf.append(SystemUtils.LINE_SEPARATOR);
            }
            else
            {
                buf.append(',').append(' ');
            }
        }

        // don't forget the last one
        Object lastItem = items.next();
        if (lastItem instanceof Class)
        {
            buf.append(((Class) lastItem).getName());
        }
        else
        {
            buf.append(lastItem);
        }

        if (newline)
        {
            buf.append(SystemUtils.LINE_SEPARATOR);
        }

        if (tooManyElements)
        {
            buf.append(" [..]");
        }

        buf.append(']');
        return buf.toString();
    }

}
