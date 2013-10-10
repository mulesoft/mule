/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.NameableObject;

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
    public static boolean getBoolean(final Object answer, Boolean defaultValue)
    {
        if (answer != null)
        {
            if (answer instanceof Boolean)
            {
                return (Boolean) answer;

            }
            else if (answer instanceof String)
            {
                return Boolean.valueOf((String) answer);

            }
            else if (answer instanceof Number)
            {
                Number n = (Number) answer;
                return (n.intValue() > 0) ? Boolean.TRUE : Boolean.FALSE;
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
    public static byte getByte(final Object answer, Byte defaultValue)
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
                return Byte.valueOf((String) answer);
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
    public static short getShort(final Object answer, Short defaultValue)
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
                return Short.valueOf((String) answer);
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
    public static int getInt(final Object answer, Integer defaultValue)
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
                return Integer.valueOf((String) answer);
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
    public static long getLong(final Object answer, Long defaultValue)
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
                return Long.valueOf((String) answer);
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
    public static float getFloat(final Object answer, Float defaultValue)
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
                return Float.valueOf((String) answer);
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
    public static double getDouble(final Object answer, Double defaultValue)
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
                return Double.valueOf((String) answer);
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

    public static String toString(Object obj)
    {
        if (obj == null)
        {
            return "";
        }

        String str = obj.getClass().getName();
        if (obj instanceof NameableObject)
        {
            str += String.format(" '%s'", ((NameableObject) obj).getName());
        }
        return str;
    }
}
