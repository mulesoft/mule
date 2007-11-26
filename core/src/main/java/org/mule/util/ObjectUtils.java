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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ObjectUtils extends org.apache.commons.lang.ObjectUtils
{

    /** logger used by this class */
    protected static final Log logger = LogFactory.getLog(ObjectUtils.class);

    /**
     * Like {@link #identityToString(Object)} but without the object's full package
     * name.
     *
     * @param obj the object for which the identity description is to be generated
     * @return the object's identity description in the form of
     *         "ClassName@IdentityCode" or "null" if the argument was null.
     */
    public static String identityToShortString(Object obj)
    {
        if (obj == null)
        {
            return "null";
        }
        else
        {
            return new StringBuffer(40).append(
                    ClassUtils.getSimpleName(obj.getClass()))
                    .append('@')
                    .append(Integer.toHexString(System.identityHashCode(obj))
                    ).toString();
        }
    }

    /**
     * Gets a String from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a String, or the defaultValue
     */
    public static String getString(final Object answer, String defaultValue)
    {
        if (answer != null)
        {
            return answer.toString();
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Gets a boolean from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a boolean, or the defaultValue
     */
    public static boolean getBoolean(final Object answer, boolean defaultValue)
    {
        if (answer != null)
        {
            if (answer instanceof Boolean)
            {
                return ((Boolean) answer).booleanValue();

            }
            else if (answer instanceof String)
            {
                return Boolean.valueOf((String) answer).booleanValue();

            }
            else if (answer instanceof Number)
            {
                Number n = (Number) answer;
                return ((n.intValue() > 0) ? Boolean.TRUE : Boolean.FALSE).booleanValue();
            }
            else
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn("Value exists but cannot be converted to boolean: "
                            + answer + ", returning default value: " + defaultValue);
                }
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a byte from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a byte, or the defaultValue
     */
    public static byte getByte(final Object answer, byte defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).byteValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Byte.valueOf((String) answer).byteValue();
            }
            catch (NumberFormatException e)
            {
                //handled below
            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to byte: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a short from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a short, or the defaultValue
     */
    public static short getShort(final Object answer, short defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).shortValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Short.valueOf((String) answer).shortValue();
            }
            catch (NumberFormatException e)
            {
                //handled below
            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to short: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a int from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a int, or the defaultValue
     */
    public static int getInt(final Object answer, int defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).intValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Integer.valueOf((String) answer).intValue();
            }
            catch (NumberFormatException e)
            {
                //handled below
            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to int: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a long from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a long, or the defaultValue
     */
    public static long getLong(final Object answer, long defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).longValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Long.valueOf((String) answer).longValue();
            }
            catch (NumberFormatException e)
            {
                //handled below

            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to long: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a float from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a float, or the defaultValue
     */
    public static float getFloat(final Object answer, float defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).floatValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Float.valueOf((String) answer).floatValue();
            }
            catch (NumberFormatException e)
            {
                //handled below

            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to float: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a double from a value in a null-safe manner.
     * <p/>
     *
     * @param answer       the object value
     * @param defaultValue the default to use if null or of incorrect type
     * @return the value as a double, or the defaultValue
     */
    public static double getDouble(final Object answer, double defaultValue)
    {
        if (answer == null)
        {
            return defaultValue;
        }
        else if (answer instanceof Number)
        {
            return ((Number) answer).doubleValue();
        }
        else if (answer instanceof String)
        {
            try
            {
                return Double.valueOf((String) answer).doubleValue();
            }
            catch (NumberFormatException e)
            {
                //handled below
            }
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("Value exists but cannot be converted to double: "
                    + answer + ", returning default value: " + defaultValue);
        }
        return defaultValue;
    }

}