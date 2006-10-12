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

import org.apache.commons.lang.SystemUtils;

// @Immutable
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils
{

    /**
     * TODO
     * 
     * @param c
     * @param newline
     * @return
     */
    public static String toString(Collection c, boolean newline)
    {
        if (c == null || c.isEmpty())
        {
            return "[]";
        }

        StringBuffer buf = new StringBuffer(c.size() * 32);
        buf.append('[');

        if (newline)
        {
            buf.append(SystemUtils.LINE_SEPARATOR);
        }

        Object[] items = c.toArray();
        int i;

        for (i = 0; i < items.length - 1; i++)
        {
            Object item = items[i];

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
        Object lastItem = items[i];
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

        buf.append(']');
        return buf.toString();
    }

}
