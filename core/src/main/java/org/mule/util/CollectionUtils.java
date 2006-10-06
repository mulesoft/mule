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

// @Immutable
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils
{
    public static String toString(Collection c, boolean newLine)
    {
        StringBuffer buf = new StringBuffer(128);
        Object item;
        for (Iterator iterator = c.iterator(); iterator.hasNext();)
        {
            item = iterator.next();
            if(item instanceof Class) {
                buf.append(((Class)item).getName()).append(", ");                
            } else {
                buf.append(item).append(", ");
            }
            if(newLine) buf.append("\n");
        }
        return buf.toString();
    }
}
