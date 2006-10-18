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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.SystemUtils;

// @ThreadSafe
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils
{

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
                buf.append(((Class)item).getName());
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
            buf.append(((Class)lastItem).getName());
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
