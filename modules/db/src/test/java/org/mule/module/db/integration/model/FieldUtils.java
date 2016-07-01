/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

/**
 * Utilities to work with field values
 */
public class FieldUtils
{

    private FieldUtils()
    {
    }

    public static String getValueAsString(Object value)
    {
        String valueString;
        if (value instanceof Object[])
        {
            valueString = toString((Object[]) value);
        }
        else
        {
            valueString = value != null ? value.toString() : "null";
        }
        return valueString;
    }

    private static String toString(Object[] value)
    {
        if (value == null)
        {
            return "null";
        }

        int iMax = value.length - 1;
        if (iMax == -1)
        {
            return "[]";
        }

        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; ; i++)
        {
            if (value[i] instanceof Object[])
            {
                builder.append(toString((Object[]) value[i]));
            }
            else
            {
                builder.append(String.valueOf(value[i]));
            }
            if (i == iMax)
            {
                return builder.append(']').toString();
            }
            builder.append(", ");
        }
    }
}
