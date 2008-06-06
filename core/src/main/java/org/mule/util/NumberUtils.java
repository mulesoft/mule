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

/**
 * <code>NumberUtils</code> contains useful methods for manipulating numbers.
 */
// @ThreadSafe
public class NumberUtils extends org.apache.commons.lang.math.NumberUtils
{
    public static final int INTEGER_ERROR = -999999999;
    public static final long LONG_ERROR = -999999999;
    public static final float FLOAT_ERROR = -999999999;
    public static final double DOUBLE_ERROR = -999999999;

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

    public static float toFloat(Object obj)
    {
        if (obj == null)
        {
            throw new IllegalArgumentException("Unable to convert null object to float");
        }
        else if (obj instanceof String)
        {
            return toFloat((String) obj);
        }
        else if (obj instanceof Number)
        {
            return ((Number) obj).floatValue();
        }
        else
        {
            throw new IllegalArgumentException("Unable to convert object of type: "
                                               + obj.getClass().getName() + " to float.");
        }
    }

    public static double toDouble(Object obj)
    {
        if (obj == null)
        {
            throw new IllegalArgumentException("Unable to convert null object to double");
        }
        else if (obj instanceof String)
        {
            return toDouble((String) obj);
        }
        else if (obj instanceof Number)
        {
            return ((Number) obj).doubleValue();
        }
        else
        {
            throw new IllegalArgumentException("Unable to convert object of type: "
                                               + obj.getClass().getName() + " to double.");
        }
    }

    //@Override
    public static int toInt(String str) {
        return toInt(str, INTEGER_ERROR);
    }

    //@Override
    public static long toLong(String str) {
        return toLong(str, LONG_ERROR);
    }

    //@Override
    public static float toFloat(String str) {
        return toFloat(str, FLOAT_ERROR);
    }
    
    //@Override
    public static double toDouble(String str) {
        return toDouble(str, DOUBLE_ERROR);
    }
}
