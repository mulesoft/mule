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

/**
 * <code>NumberUtils</code> contains useful methods for manipulating numbers.
 */
// @ThreadSafe
public class NumberUtils extends org.apache.commons.lang.math.NumberUtils
{

    public static long toLong(Object obj)
    {
        if (obj == null)
        {
            throw new IllegalArgumentException("Unable to convert null object to long");
        }
        else if (obj instanceof String)
        {
            return toLong((String) obj);
        }
        else if (obj instanceof Number)
        {
            return ((Number) obj).longValue();
        }
        else
        {
            throw new IllegalArgumentException("Unable to convert object of type: "
                                               + obj.getClass().getName() + " to long.");
        }
    }

    public static int toInt(Object obj)
    {
        if (obj == null)
        {
            throw new IllegalArgumentException("Unable to convert null object to int");
        }
        else if (obj instanceof String)
        {
            return toInt((String) obj);
        }
        else if (obj instanceof Number)
        {
            return ((Number) obj).intValue();
        }
        else
        {
            throw new IllegalArgumentException("Unable to convert object of type: "
                                               + obj.getClass().getName() + " to int.");
        }
    }

}
